package ru.ekuzmichev
package consumer

import bot.OzonPriceCheckerBotCommands
import common.{ChatId, ProductId, UserName}
import product.{ProductFetcher, ProductIdParser}
import schedule.Cron4sNextTimeProvider.fromCronString
import schedule.{Cron4sNextTimeProvider, ZioScheduler}
import store.*
import store.ProductStore.ProductCandidate.*
import store.ProductStore.{Product, ProductCandidate, SourceId}
import util.zio.ZioLoggingImplicits.Ops

import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.message.Message
import org.telegram.telegrambots.meta.generics.TelegramClient
import zio.{LogAnnotation, Runtime, Task, ZIO}

class OzonPriceCheckerUpdateConsumer(
    telegramClient: TelegramClient,
    productStore: ProductStore,
    productFetcher: ProductFetcher,
    zioScheduler: ZioScheduler,
    productIdParser: ProductIdParser,
    runtime: Runtime[Any]
) extends ZioLongPollingSingleThreadUpdateConsumer(runtime):

  override def consumeZio(update: Update): Task[Unit] =
    ZIO
      .when(update.hasMessage)(processMessage(update.getMessage))
      .unit

  private def processMessage(message: Message): Task[Unit] =
    val chatId: ChatId     = message.getChatId.toString
    val userName: UserName = message.getFrom.getUserName

    val sourceId: SourceId = SourceId(userName, chatId)

    ZIO
      .logAnnotate(LogAnnotation("userName", userName), LogAnnotation("chatId", chatId)) {
        ZIO.log(s"Received: $message").zipRight {
          ZIO.when(message.hasText) {
            val text: String = message.getText
            if (message.isCommand)
              processCommand(sourceId, text)
            else
              processText(sourceId, text)
          }
        }
      }
      .unit

  private def processText(sourceId: SourceId, text: ChatId): Task[Unit] =
    def sendDefaultMsg() = sendTextMessage(sourceId.chatId, "Send me a command known to me.")

    def onProductId(productId: ProductId) = {
      ZIO
        .attempt(productFetcher.fetchProductInfo(productId))
        .tap(productInfo =>
          sendTextMessage(
            sourceId.chatId,
            s"I have fetched product info from OZON for you:\n\n" +
              s"ID: $productId\n" +
              s"Name: ${productInfo.name}\n\n" +
              s"Current Price: ${productInfo.price} ₽\n\n" +
              s"Send me price threshold."
          )
        )
        .flatMap(productInfo =>
          productStore.updateProductCandidate(sourceId, WaitingPriceThreshold(productId, productInfo.price))
        )
    }

    def onPriceThreshold(productId: ProductId, priceThreshold: Int) = {
      sendTextMessage(
        sourceId.chatId,
        s"I have got price threshold from you:\n\n" +
          s"ID: $productId\n\n" +
          s"Price Threshold: $priceThreshold ₽\n\n" +
          s"Added product to watches.\n\n" +
          s"To watch new product send me ${OzonPriceCheckerBotCommands.WatchNewProduct}"
      ) *>
        productStore.resetProductCandidate(sourceId) *>
        productStore.addProduct(sourceId, Product(productId, priceThreshold)) *>
        productStore
          .readSourceState(sourceId)
          .map(_.exists(_.products.size == 1))
          .tap {
            ZIO.when(_)(
              ZIO
                .fromTry(fromCronString("0 */1 * * * ?"))
                .logged("create cron4s time provider", printResult = _.info)
                .flatMap {
                  zioScheduler
                    .schedule(
                      productStore
                        .readSourceState(sourceId)
                        .map(_.fold(Seq.empty[Product])(_.products))
                        .flatMap { products =>
                          sendTextMessage(
                            sourceId.chatId,
                            s"Here are your watched products:\n${products.mkString("\n")}"
                          )
                        },
                      _,
                      s"${sourceId.userName}|${sourceId.chatId}"
                    )
                    .forkDaemon
                }
                .ignore
            )
          }
    }

    for {
      _ <- ZIO.log(s"Got text '$text'")

      maybeSourceState <- productStore.readSourceState(sourceId)

      _ <- maybeSourceState match
        case Some(sourceState) =>
          sourceState.maybeProductCandidate match
            case Some(productCandidate) =>
              productCandidate match
                case WaitingProductId =>
                  productIdParser.parse(text) match
                    case Right(productId) =>
                      onProductId(productId)
                    case Left(error) =>
                      ZIO.log(s"Failed to parse productId from text '$text'. Cause: $error") *>
                        sendTextMessage(sourceId.chatId, s"Send me valid URL or product ID")
                case WaitingPriceThreshold(productId, productPrice) =>
                  text.toIntOption match
                    case Some(priceThreshold) =>
                      if (priceThreshold < productPrice)
                        onPriceThreshold(productId, priceThreshold)
                      else
                        sendTextMessage(
                          sourceId.chatId,
                          s"Product '$productId' price $productPrice ₽ is already <= price threshold $priceThreshold ₽.\n\n" +
                            s"Send new price threshold < $productPrice ₽ or ${OzonPriceCheckerBotCommands.Cancel}"
                        )
                    case None =>
                      sendTextMessage(sourceId.chatId, "Send me valid price as a number")
            case None =>
              sendDefaultMsg()
        case None =>
          sendDefaultMsg()
    } yield ()

  private def sendTextMessage(chatId: ChatId, message: String): Task[Unit] =
    ZIO.attempt {
      val sendMessage = new SendMessage(chatId, message)
      telegramClient.execute(sendMessage)
    }.unit

  private def processCommand(sourceId: SourceId, text: ChatId): Task[Unit] =
    if (text == OzonPriceCheckerBotCommands.Start)
      processStartCommand(sourceId)
    else if (text == OzonPriceCheckerBotCommands.Stop)
      processStopCommand(sourceId)
    else if (text == OzonPriceCheckerBotCommands.Cancel)
      processCancelCommand(sourceId)
    else if (text == OzonPriceCheckerBotCommands.WatchNewProduct)
      processWatchNewProductCommand(sourceId)
    else if (text == OzonPriceCheckerBotCommands.UnwatchAllProducts)
      processUnwatchAllProductsCommand(sourceId)
    else
      ZIO.log(s"Got unknown command $text") *>
        sendTextMessage(sourceId.chatId, s"I can not process command $text. Please send me a command known to me.")

  private def processStartCommand(sourceId: SourceId): Task[Unit] =
    initializeStoreEntry(sourceId)
      .flatMap(initialized =>
        val msg =
          if (initialized)
            "I have added you to the Store."
          else
            "You have been already added to the Store before."
        sendTextMessage(sourceId.chatId, msg)
      )
      .logged(s"process command ${OzonPriceCheckerBotCommands.Start} ")

  private def initializeStoreEntry(sourceId: SourceId): Task[Boolean] =
    for {
      initialized <- productStore.checkInitialized(sourceId)

      _ <- ZIO.log(s"Source ID is ${if (initialized) "already" else "not"} initialized in store")

      _ <- ZIO.when(!initialized) {
        productStore.emptyState(sourceId).logged(s"initialize store entry")
      }
    } yield !initialized

  private def processStopCommand(sourceId: SourceId): Task[Unit] =
    (for {
      initialized <- productStore.checkInitialized(sourceId)
      _           <- ZIO.when(initialized)(productStore.clearState(sourceId))
      msg =
        if (initialized) "I have deleted you from the Store."
        else "You have been already removed from the Store before."
      _ <- sendTextMessage(sourceId.chatId, msg)
    } yield ())
      .logged(s"process command ${OzonPriceCheckerBotCommands.Stop} ")

  private def processCancelCommand(sourceId: SourceId): Task[Unit] =
    productStore.resetProductCandidate(sourceId) *>
      sendTextMessage(
        sourceId.chatId,
        "Current product addition has been cancelled.\n\n" +
          s"Send me ${OzonPriceCheckerBotCommands.WatchNewProduct} to start over"
      )

  private def processWatchNewProductCommand(sourceId: SourceId): Task[Unit] =
    (for {
      initialized <- productStore.checkInitialized(sourceId)
      _ <-
        if (initialized)
          sendTextMessage(sourceId.chatId, "Send me OZON URL or product ID.") *>
            productStore.updateProductCandidate(sourceId, WaitingProductId)
        else
          sendTextMessage(
            sourceId.chatId,
            s"You have not been yet initialized. Send me ${OzonPriceCheckerBotCommands.Start} command to fix it."
          )
    } yield ())
      .logged(s"process command ${OzonPriceCheckerBotCommands.WatchNewProduct}")

  private def processUnwatchAllProductsCommand(sourceId: SourceId): Task[Unit] =
    productStore
      .emptyState(sourceId)
      .zipRight(sendTextMessage(sourceId.chatId, "I have removed all watched products."))
      .logged(s"process command ${OzonPriceCheckerBotCommands.UnwatchAllProducts}")

end OzonPriceCheckerUpdateConsumer
