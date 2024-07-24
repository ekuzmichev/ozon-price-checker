package ru.ekuzmichev
package bot

import common.{ChatId, ProductId, UserName}
import store.*
import store.ProductStore.SourceId
import util.zio.ZioLoggingImplicits.Ops

import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.message.Message
import org.telegram.telegrambots.meta.generics.TelegramClient
import zio.{LogAnnotation, Ref, Runtime, Task, ZIO}

class OzonPriceCheckerBot(
    telegramClient: TelegramClient,
    productStore: ProductStore,
    runtime: Runtime[Any]
) extends ZioLongPollingSingleThreadUpdateConsumer(runtime) {

  override def consumeZio(update: Update): Task[Unit] =
    ZIO
      .when(update.hasMessage)(processMessage(update.getMessage))
      .unit

  private def processMessage(message: Message): Task[Unit] = {
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
  }

  private def processText(sourceId: SourceId, text: ChatId): Task[Unit] =
    ZIO.log(s"Got text $text") *>
      sendTextMessage(sourceId.chatId, "I am able to process only commands. Send me one known to me.")

  private def processCommand(sourceId: SourceId, text: ChatId): Task[Unit] =
    if (text == Commands.Start) {
      processStartCommand(sourceId)
    } else if (text == Commands.Stop) {
      processStopCommand(sourceId)
    } else if (text == Commands.WatchNewProduct) {
      processWatchNewProductCommand(sourceId)
    } else if (text == Commands.UnwatchAllProducts) {
      processUnwatchAllProductsCommand(sourceId)
    } else {
      ZIO.log(s"Got unknown command $text") *>
        sendTextMessage(sourceId.chatId, s"I can not process command $text. Please send me a command known to me.")
    }

  private def processStartCommand(sourceId: SourceId): Task[Unit] =
    initializeStoreEntry(sourceId)
      .flatMap(initialized => {
        val msg =
          if (initialized)
            "I have added you to the Store."
          else
            "You have been already added to the Store before."
        sendTextMessage(sourceId.chatId, msg)
      })
      .logged(s"process command ${Commands.Start} ")

  private def initializeStoreEntry(sourceId: SourceId): Task[Boolean] =
    for {
      initialized <- productStore.checkInitialized(sourceId)

      _ <- ZIO.log(s"Source ID $sourceId is ${if (initialized) "already" else "not"} initialized in store")

      _ <- ZIO.when(!initialized) {
        productStore.emptyState(sourceId).logged(s"initialize source ID $sourceId store entry")
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
      .logged(s"process command ${Commands.Stop} ")

  private def processWatchNewProductCommand(sourceId: SourceId): Task[Unit] =
    (for {
      initialized <- productStore.checkInitialized(sourceId)
      _ <-
        if (initialized) {
          sendTextMessage(sourceId.chatId, "Send me OZON URL or product ID.")
        } else {
          sendTextMessage(
            sourceId.chatId,
            s"You have not been yet initialized. Send me ${Commands.Start} command to fix it."
          )
        }
    } yield ())
      .logged(s"process command ${Commands.WatchNewProduct}")

  private def sendTextMessage(chatId: ChatId, message: String): Task[Unit] =
    ZIO.attempt {
      val sendMessage = new SendMessage(chatId, message)
      telegramClient.execute(sendMessage)
    }.unit

  private def processUnwatchAllProductsCommand(sourceId: SourceId): Task[Unit] =
    productStore
      .emptyState(sourceId)
      .zipRight(sendTextMessage(sourceId.chatId, "I have removed all watched products."))
      .logged(s"process command ${Commands.UnwatchAllProducts}")

}
