package ru.ekuzmichev
package bot

import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer
import org.telegram.telegrambots.meta.api.objects.Update
import zio.{Runtime, Task, Unsafe, ZIO}

abstract class ZioLongPollingSingleThreadUpdateConsumer(runtime: Runtime[Any]) extends LongPollingSingleThreadUpdateConsumer {
  override def consume(update: Update): Unit =
    Unsafe.unsafe { implicit unsafe =>
      runtime.unsafe.run {
        consumeZio(update)
          .catchAll(error => ZIO.logError(s"Failed to process update: $update. Cause: $error"))
      }
    }

  def consumeZio(update: Update): Task[Unit]
}
