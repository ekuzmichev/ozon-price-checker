package ru.ekuzmichev
package consumer

import bot.CallbackData.DeleteProduct
import bot.{CallbackData, OzonPriceCheckerBotCommands}
import common.{ChatId, ProductId, UserName}
import product.{ProductFetcher, ProductIdParser}
import store.*
import store.ProductStore.ProductCandidate.*
import store.ProductStore.{Product, ProductCandidate, SourceId}
import util.lang.Throwables
import util.telegram.MessageSendingUtils.sendTextMessage

import cats.syntax.either.*
import io.circe.parser.decode
import org.telegram.telegrambots.meta.api.objects.message.Message
import org.telegram.telegrambots.meta.api.objects.{CallbackQuery, Update}
import org.telegram.telegrambots.meta.generics.TelegramClient
import zio.{LogAnnotation, Runtime, Task, ZIO}

class OzonPriceCheckerUpdateConsumer(
    telegramClient: TelegramClient,
    productStore: ProductStore,
    productFetcher: ProductFetcher,
    productIdParser: ProductIdParser,
    commandProcessor: CommandProcessor,
    productWatchingJobScheduler: ProductWatchingJobScheduler,
    runtime: Runtime[Any]
) extends ZioLongPollingSingleThreadUpdateConsumer(runtime):

  private implicit val _telegramClient: TelegramClient = telegramClient

  // noinspection SimplifyWhenInspection
  override def consumeZio(update: Update): Task[Unit] =
    if update.hasMessage then processMessage(update.getMessage)
    else if update.hasCallbackQuery then processCallbackQuery(update.getCallbackQuery)
    else ZIO.unit

  private def processMessage(message: Message): Task[Unit] =
    val chatId: ChatId     = message.getChatId.toString
    val userName: UserName = message.getFrom.getUserName

    val sourceId: SourceId = SourceId(userName, chatId)

    ZIO
      .logAnnotate(LogAnnotation("userName", userName), LogAnnotation("chatId", chatId)) {
        ZIO.log(s"Received: $message").zipRight {
          ZIO.when(message.hasText) {
            val text: String = message.getText
            if message.isCommand then commandProcessor.processCommand(sourceId, text)
            else processText(sourceId, text)
          }
        }
      }
      .unit

  private def processText(sourceId: SourceId, text: ChatId): Task[Unit] =
    def sendDefaultMsg() = sendTextMessage(sourceId.chatId, "Send me a command known to me.")

    for
      _ <- ZIO.log(s"Got text '$text'")

      maybeSourceState <- productStore.readSourceState(sourceId)

      _ <- maybeSourceState match
        case Some(sourceState) =>
          sourceState.maybeProductCandidate match
            case Some(productCandidate) =>
              productCandidate match
                case WaitingProductId =>
                  productIdParser.parse(text).flatMap {
                    case Right(productId) =>
                      onProductId(sourceId, productId)
                    case Left(error) =>
                      ZIO.log(s"Failed to parse productId from text '$text'. Cause: $error") *>
                        sendTextMessage(sourceId.chatId, s"Send me valid URL or product ID")
                  }
                case WaitingPriceThreshold(productId, productPrice) =>
                  text.toIntOption match
                    case Some(priceThreshold) =>
                      if priceThreshold < productPrice then onPriceThreshold(sourceId, productId, priceThreshold)
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
    yield ()

  private def onProductId(sourceId: SourceId, productId: ProductId) =
    productStore.checkHasProductId(sourceId, productId).flatMap {
      case true =>
        ZIO.logDebug(s"Source ID $sourceId already contains product with ID $productId") *>
          sendTextMessage(
            sourceId.chatId,
            s"You have already added product with ID $productId.\n\n" +
              s"Send me another product ID or command ${OzonPriceCheckerBotCommands.Cancel}"
          )
      case false =>
        productFetcher
          .fetchProductInfo(productId)
          .tap {
            case Some(productInfo) =>
              sendTextMessage(
                sourceId.chatId,
                s"I have fetched product info from OZON for you:\n\n" +
                  s"ID: $productId\n" +
                  s"Name: ${productInfo.name}\n\n" +
                  s"Current Price: ${productInfo.price} ₽\n\n" +
                  s"Send me price threshold."
              ) *>
                productStore.updateProductCandidate(sourceId, WaitingPriceThreshold(productId, productInfo.price))

            case None =>
              sendTextMessage(
                sourceId.chatId,
                s"I can not retrieve information about product with ID $productId.\n\n" +
                  s"Send me another product ID or command ${OzonPriceCheckerBotCommands.Cancel}"
              )
          }

    }

  private def processCallbackQuery(callbackQuery: CallbackQuery): Task[Unit] =
    val data     = callbackQuery.getData
    val userName = callbackQuery.getFrom.getUserName
    val chatId   = callbackQuery.getMessage.getChatId.toString

    val sourceId = SourceId(userName, chatId)

    ZIO
      .logAnnotate(LogAnnotation("userName", userName), LogAnnotation("chatId", chatId)) {
        ZIO.log(s"Callback data: $data") *>
          ZIO
            .fromEither(
              decode[CallbackData](data).leftMap(error => Throwables.failure(s"Failed to decode callback data: $error"))
            )
            .flatMap { case DeleteProduct(productIndex) =>
              ZIO.log(s"Removing product with index = $productIndex") *>
                productStore.removeProduct(sourceId, productIndex) <*
                sendTextMessage(
                  chatId,
                  s"The product at index $productIndex has been removed. \n\n" +
                    s"Check the actual list of watched products with ${OzonPriceCheckerBotCommands.ShowAllProducts}"
                )
            }
            .unit
      }

  private def onPriceThreshold(sourceId: SourceId, productId: ProductId, priceThreshold: Int) =
    sendTextMessage(
      sourceId.chatId,
      s"I have got price threshold from you:\n\n" +
        s"ID: $productId\n\n" +
        s"Price Threshold: $priceThreshold ₽\n\n" +
        s"Added product to watches.\n\n" +
        s"To watch new product send me ${OzonPriceCheckerBotCommands.WatchNewProduct}"
    ) *>
      productStore.resetProductCandidate(sourceId) *>
      productStore.addProduct(sourceId, Product(productId, priceThreshold))

end OzonPriceCheckerUpdateConsumer
