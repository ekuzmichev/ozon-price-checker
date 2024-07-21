package ru.ekuzmichev
package bot

import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer
import org.telegram.telegrambots.meta.api.objects.Update

class OzonPriceCheckerBot extends LongPollingSingleThreadUpdateConsumer {
  override def consume(update: Update): Unit =
    println(s"Update: $update")
    if (update.hasMessage && update.getMessage.hasText)
      println(s"Text: ${update.getMessage.getText}")
}
