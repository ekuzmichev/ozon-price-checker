package ru.ekuzmichev
package bot

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
        .unit
}
