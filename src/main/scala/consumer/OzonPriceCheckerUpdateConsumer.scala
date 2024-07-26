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
import util.telegram.MessageSendingUtils.sendTextMessage
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
    commandProcessor: CommandProcessor,
    runtime: Runtime[Any]
) extends ZioLongPollingSingleThreadUpdateConsumer(runtime):

  private implicit val _telegramClient: TelegramClient = telegramClient

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
              commandProcessor.processCommand(sourceId, text)
            else
              processText(sourceId, text)
          }
        }
      }
      .unit

  private def processText(sourceId: SourceId, text: ChatId): Task[Unit] =
    def sendDefaultMsg() = sendTextMessage(sourceId.chatId, "Send me a command known to me.")

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
                      onProductId(sourceId, productId)
                    case Left(error) =>
                      ZIO.log(s"Failed to parse productId from text '$text'. Cause: $error") *>
                        sendTextMessage(sourceId.chatId, s"Send me valid URL or product ID")
                case WaitingPriceThreshold(productId, productPrice) =>
                  text.toIntOption match
                    case Some(priceThreshold) =>
                      if (priceThreshold < productPrice)
                        onPriceThreshold(sourceId, productId, priceThreshold)
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

  private def onProductId(sourceId: SourceId, productId: ProductId) = {
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

  private def onPriceThreshold(sourceId: SourceId, productId: ProductId, priceThreshold: Int) = {
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
      hasFirstProductAdded(sourceId)
        .tap(ZIO.when(_)(scheduleProductsWatching(sourceId)))
  }

  private def scheduleProductsWatching(sourceId: SourceId) = {
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
  }

  private def hasFirstProductAdded(sourceId: SourceId) = {
    productStore
      .readSourceState(sourceId)
      .map(_.exists(_.products.size == 1))
  }

end OzonPriceCheckerUpdateConsumer
