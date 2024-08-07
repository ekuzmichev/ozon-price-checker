package ru.ekuzmichev
package consumer

import bot.OzonPriceCheckerBotCommands
import store.ProductStore
import store.ProductStore.ProductCandidate.WaitingProductId
import store.ProductStore.SourceId
import util.telegram.MessageSendingUtils.sendTextMessage
import util.zio.ZioLoggingImplicits.Ops

import org.telegram.telegrambots.meta.generics.TelegramClient
import zio.{Task, ZIO}

class OzonPriceCheckerCommandProcessor(productStore: ProductStore, telegramClient: TelegramClient)
    extends CommandProcessor:
  private implicit val _telegramClient: TelegramClient = telegramClient

  def processCommand(sourceId: SourceId, text: String): Task[Unit] =
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
    else if (text == OzonPriceCheckerBotCommands.ShowAllProducts)
      processShowAllProductsCommand(sourceId)
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
          askToSendStartCommand(sourceId)
    } yield ())
      .logged(s"process command ${OzonPriceCheckerBotCommands.WatchNewProduct}")

  private def processUnwatchAllProductsCommand(sourceId: SourceId): Task[Unit] =
    productStore
      .emptyState(sourceId)
      .zipRight(sendTextMessage(sourceId.chatId, "I have removed all watched products."))
      .logged(s"process command ${OzonPriceCheckerBotCommands.UnwatchAllProducts}")

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
                  s"To watch new product send me ${OzonPriceCheckerBotCommands.WatchNewProduct}"
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
      .logged(s"process command ${OzonPriceCheckerBotCommands.ShowAllProducts}")
      .unit

  private def askToSendStartCommand(sourceId: SourceId) =
    sendTextMessage(
      sourceId.chatId,
      s"You have not been yet initialized. Send me ${OzonPriceCheckerBotCommands.Start} command to fix it."
    )
