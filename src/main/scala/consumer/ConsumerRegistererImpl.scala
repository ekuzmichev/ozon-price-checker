package ru.ekuzmichev
package consumer

import consumer.OzonPriceCheckerUpdateConsumer
import product.OzonProductFetcher
import schedule.{AlphaNumericJobIdGenerator, ZioSchedulerImpl}
import store.InMemoryProductStore
import store.InMemoryProductStore.ProductState
import util.lang.Throwables.makeCauseSeqMessage
import util.zio.ZioLoggingImplicits.Ops

import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer
import org.telegram.telegrambots.longpolling.{BotSession, TelegramBotsLongPollingApplication}
import zio.{Ref, Task, ZIO}

class ConsumerRegistererImpl extends ConsumerRegisterer:
  override def registerConsumer(
      botsApplication: TelegramBotsLongPollingApplication,
      longPollingUpdateConsumer: LongPollingUpdateConsumer,
      token: String
  ): Task[BotSession] =
    for {
      botSession <- ZIO
        .attempt(botsApplication.registerBot(token, longPollingUpdateConsumer))
        .logged(
          s"register updates consumer ${longPollingUpdateConsumer.getClass.getSimpleName}",
          printResult = session => s"Session is ${if (session.isRunning) "" else "not "}running"
        )
    } yield botSession
