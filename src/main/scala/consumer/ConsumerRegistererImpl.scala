package ru.ekuzmichev
package consumer

import common.Sensitive
import util.zio.ZioLoggingImplicits.Ops

import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer
import org.telegram.telegrambots.longpolling.{BotSession, TelegramBotsLongPollingApplication}
import zio.{Task, ZIO}

class ConsumerRegistererImpl extends ConsumerRegisterer:
  override def registerConsumer(
      botsApplication: TelegramBotsLongPollingApplication,
      longPollingUpdateConsumer: LongPollingUpdateConsumer,
      token: Sensitive[String]
  ): Task[BotSession] =
    for botSession <- ZIO
        .attempt(botsApplication.registerBot(token.value, longPollingUpdateConsumer))
        .logged(
          s"register updates consumer ${longPollingUpdateConsumer.getClass.getSimpleName}",
          printResult = session => s"Session is ${if session.isRunning then "" else "not "}running"
        )
    yield botSession
