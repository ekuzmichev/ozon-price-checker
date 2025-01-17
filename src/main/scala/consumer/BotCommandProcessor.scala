package ru.ekuzmichev
package consumer

import bot.CallbackData.DeleteProduct
import bot.{BotAdminCommands, BotGeneralCommands, CallbackData}
import store.ProductStore
import store.ProductStore.ProductCandidate.WaitingProductId
import store.ProductStore.{SourceId, SourceState}
import util.telegram.MessageSendingUtils.sendTextMessage
import util.zio.ZioLoggingImplicits.Ops

import io.circe.syntax.*
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.{InlineKeyboardButton, InlineKeyboardRow}
import org.telegram.telegrambots.meta.generics.TelegramClient
import zio.{Task, ZIO}

class BotCommandProcessor(productStore: ProductStore, telegramClient: TelegramClient, admins: Seq[String])
    extends CommandProcessor:

  private implicit val _telegramClient: TelegramClient = telegramClient

  def processCommand(sourceId: SourceId, text: String): Task[Unit] =
    if text == BotGeneralCommands.Start then processStartCommand(sourceId)
    else if text == BotGeneralCommands.Stop then processStopCommand(sourceId)
    else if text == BotGeneralCommands.Cancel then processCancelCommand(sourceId)
    else if text == BotGeneralCommands.WatchNewProduct then processWatchNewProductCommand(sourceId)
    else if text == BotGeneralCommands.UnwatchProduct then processUnwatchProductCommand(sourceId)
    else if text == BotGeneralCommands.UnwatchAllProducts then processUnwatchAllProductsCommand(sourceId)
    else if text == BotGeneralCommands.ShowAllProducts then processShowAllProductsCommand(sourceId)
    else if text == BotAdminCommands.Stats && admins.contains(sourceId.userName) then processStatsCommand(sourceId)
    else
      ZIO.log(s"Got unknown command $text") *>
        sendTextMessage(sourceId.chatId, s"I can not process command $text. Please send me a command known to me.")

  private def processStartCommand(sourceId: SourceId): Task[Unit] =
    initializeStoreEntry(sourceId)
      .flatMap(initialized =>
        val msg =
          if initialized then "I have added you to the Store."
          else "You have been already added to the Store before."
        sendTextMessage(sourceId.chatId, msg)
      )
      .logged(s"process command ${BotGeneralCommands.Start} ")

  private def initializeStoreEntry(sourceId: SourceId): Task[Boolean] =
    for
      initialized <- productStore.checkInitialized(sourceId)

      _ <- ZIO.log(s"Source ID is ${if initialized then "already" else "not"} initialized in store")

      _ <- ZIO.when(!initialized) {
        productStore.emptyState(sourceId).logged(s"initialize store entry")
      }
    yield !initialized

  private def processStopCommand(sourceId: SourceId): Task[Unit] =
    (for
      initialized <- productStore.checkInitialized(sourceId)
      _           <- ZIO.when(initialized)(productStore.clearState(sourceId))
      msg =
        if initialized then "I have deleted you from the Store."
        else "You have been already removed from the Store before."
      _ <- sendTextMessage(sourceId.chatId, msg)
    yield ())
      .logged(s"process command ${BotGeneralCommands.Stop} ")

  private def processCancelCommand(sourceId: SourceId): Task[Unit] =
    productStore.resetProductCandidate(sourceId) *>
      sendTextMessage(
        sourceId.chatId,
        "Current product addition has been cancelled.\n\n" +
          s"Send me ${BotGeneralCommands.WatchNewProduct} to start over"
      )

  private def processWatchNewProductCommand(sourceId: SourceId): Task[Unit] =
    (for
      initialized <- productStore.checkInitialized(sourceId)
      _ <-
        if initialized then
          sendTextMessage(sourceId.chatId, "Send me OZON URL or product ID.") *>
            productStore.updateProductCandidate(sourceId, WaitingProductId)
        else askToSendStartCommand(sourceId)
    yield ())
      .logged(s"process command ${BotGeneralCommands.WatchNewProduct}")

  private def processUnwatchAllProductsCommand(sourceId: SourceId): Task[Unit] =
    productStore
      .emptyState(sourceId)
      .zipRight(sendTextMessage(sourceId.chatId, "I have removed all watched products."))
      .logged(s"process command ${BotGeneralCommands.UnwatchAllProducts}")

  private def processUnwatchProductCommand(sourceId: SourceId): Task[Unit] =
    productStore
      .readSourceState(sourceId)
      .flatMap {
        case Some(sourceState) => replyWithInnerDeletionKeyboard(sourceId, sourceState.products)
        case None              => askToSendStartCommand(sourceId)
      }
      .logged(s"process command ${BotGeneralCommands.UnwatchProduct}")

  private def replyWithInnerDeletionKeyboard(sourceId: SourceId, products: Seq[ProductStore.Product]): Task[Unit] =
    val rows: Seq[InlineKeyboardRow] = products.zipWithIndex.map { case (product, index) =>
      new InlineKeyboardRow(
        InlineKeyboardButton
          .builder()
          .text(s"$index) ${product.id} | ${product.priceThreshold} ₽")
          .callbackData(DeleteProduct(index).asInstanceOf[CallbackData].asJson.noSpaces)
          .build()
      )
    }

    val inlineKeyboardMarkup: InlineKeyboardMarkup =
      rows
        .foldLeft(
          InlineKeyboardMarkup
            .builder()
        ) { case (curr, next) => curr.keyboardRow(next) }
        .build()

    val sendMessage: SendMessage = SendMessage
      .builder()
      .chatId(sourceId.chatId)
      .text("Choose which product you want to unwatch:")
      .replyMarkup(inlineKeyboardMarkup)
      .build()

    ZIO.attempt {
      telegramClient.execute(sendMessage)
    }.unit

  private def processShowAllProductsCommand(sourceId: SourceId): Task[Unit] =
    productStore
      .readSourceState(sourceId)
      .tap {
        case Some(sourceState) =>
          sourceState.products match
            case Nil =>
              sendTextMessage(
                sourceId.chatId,
                s"You have no watched products.\n\n" +
                  s"To watch new product send me ${BotGeneralCommands.WatchNewProduct}"
              )
            case products =>
              sendTextMessage(
                sourceId.chatId,
                s"Here are your watched products:\n\n" +
                  s"${products.zipWithIndex
                      .map { case (product, index) =>
                        s"${index + 1}) ${product.id}\n\t" +
                          s"Price threshold: ${product.priceThreshold} ₽"
                      }
                      .mkString("\n")}"
              )
        case None =>
          askToSendStartCommand(sourceId)
      }
      .logged(s"process command ${BotGeneralCommands.ShowAllProducts}")
      .unit

  private def askToSendStartCommand(sourceId: SourceId) =
    sendTextMessage(
      sourceId.chatId,
      s"You have not been yet initialized. Send me ${BotGeneralCommands.Start} command to fix it."
    )

  private def processStatsCommand(sourceId: SourceId): Task[Unit] =
    productStore
      .readAll()
      .tap { (sourceStateBySourceId: Map[SourceId, SourceState]) =>
        sendTextMessage(
          sourceId.chatId,
          s"Here are bot user statistics:\n\n" +
            s"${sourceStateBySourceId.zipWithIndex
                .map { case ((sourceId, sourceState), index) =>
                  s"${index + 1}) ${sourceId.userName} - ${sourceState.products.size} products"
                }
                .mkString("\n")}"
        )
      }
      .unit
