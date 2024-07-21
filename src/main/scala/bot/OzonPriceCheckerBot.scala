package ru.ekuzmichev
package bot

import org.telegram.telegrambots.meta.api.objects.Update
import zio.{Runtime, Task, ZIO}

class OzonPriceCheckerBot(runtime: Runtime[Any]) extends ZioLongPollingSingleThreadUpdateConsumer(runtime) {
  override def consumeZio(update: Update): Task[Unit] =
    ZIO.log(s"Update: $update") *>
      ZIO.when(update.hasMessage && update.getMessage.hasText)(ZIO.log(s"Text: ${update.getMessage.getText}")).unit
}
