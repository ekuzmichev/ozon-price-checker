package ru.ekuzmichev
package bot

import common.TypeAliases.{ChatId, ProductId, UserName}
import util.zio.ZioLoggingImplicits.Ops

import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.message.Message
import org.telegram.telegrambots.meta.generics.TelegramClient
import zio.{LogAnnotation, Ref, Runtime, Task, ZIO}

class OzonPriceCheckerBot(
    telegramClient: TelegramClient,
    productWatchStoreRef: Ref[ProductWatchStore],
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
      .flatMap(initialized =>
        sendTextMessage(
          sourceId.chatId,
          if (initialized)
            "I have added you to the Store."
          else
            "You have been already added to the Store before."
        )
      )
      .logged(s"process command ${Commands.Start} ")

  private def initializeStoreEntry(sourceId: SourceId): Task[Boolean] =
    for {
      initialized <- checkInitialized(sourceId)

      _ <- ZIO.log(s"Source ID $sourceId is ${if (initialized) "already" else "not"} initialized in store")

      _ <- ZIO.when(!initialized) {
        setToInitialState(sourceId).logged(s"initialize source ID $sourceId store entry")
      }
    } yield !initialized

  private def processStopCommand(sourceId: SourceId): Task[Unit] =
    productWatchStoreRef
      .update(_ - sourceId)
      .zipRight(sendTextMessage(sourceId.chatId, "I have deleted you from the Store."))
      .logged(s"process command ${Commands.Stop} ")

  private def processWatchNewProductCommand(sourceId: SourceId): Task[Unit] =
    (for {
      initialized <- checkInitialized(sourceId)
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

  private def checkInitialized(sourceId: SourceId): Task[Boolean] =
    productWatchStoreRef.get.map(_.contains(sourceId))

  private def processUnwatchAllProductsCommand(sourceId: SourceId): Task[Unit] =
    setToInitialState(sourceId)
      .zipRight(sendTextMessage(sourceId.chatId, "I have removed all watched products."))
      .logged(s"process command ${Commands.UnwatchAllProducts}")

  private def setToInitialState(sourceId: SourceId) =
    productWatchStoreRef.update(_ + (sourceId -> Map.empty[ProductId, WatchParams]))
}
