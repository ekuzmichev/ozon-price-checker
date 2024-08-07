package ru.ekuzmichev
package consumer

import common.Sensitive

import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer
import org.telegram.telegrambots.longpolling.{BotSession, TelegramBotsLongPollingApplication}
import zio.Task

trait ConsumerRegisterer:
  def registerConsumer(
      botsApplication: TelegramBotsLongPollingApplication,
      longPollingUpdateConsumer: LongPollingUpdateConsumer,
      token: Sensitive[String]
  ): Task[BotSession]
