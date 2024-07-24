package ru.ekuzmichev
package consumer

import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer
import org.telegram.telegrambots.longpolling.{BotSession, TelegramBotsLongPollingApplication}
import zio.Task

trait ConsumerRegisterer:
  def registerConsumer(
      botsApplication: TelegramBotsLongPollingApplication,
      longPollingUpdateConsumer: LongPollingUpdateConsumer,
      token: String
  ): Task[BotSession]
