package ru.ekuzmichev
package bot

import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.generics.TelegramClient
import zio.{Runtime, Task, ZIO}

class OzonPriceCheckerBot(telegramClient: TelegramClient, runtime: Runtime[Any])
    extends ZioLongPollingSingleThreadUpdateConsumer(runtime) {

  override def consumeZio(update: Update): Task[Unit] =
    ZIO.log(s"Received: $update") *>
      ZIO
        .when(update.hasMessage && update.getMessage.hasText)(
          ZIO.log(s"Text of the update: ${update.getMessage.getText}")
        )
      *> ZIO.attempt {
        val msg = new SendMessage(update.getMessage.getChatId.toString, update.getMessage.getText)
        telegramClient.execute(msg)
      }.unit
}
