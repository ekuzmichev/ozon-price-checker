package ru.ekuzmichev
package app

import config.AppConfig
import consumer.*
import telegram.BotsApplicationScopes
import util.lang.Throwables.makeCauseSeqMessage
import util.zio.ZioClock.getCurrentDateTime

import org.telegram.telegrambots.longpolling.BotSession
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer
import zio.logging.backend.SLF4J
import zio.{RIO, Scope, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer}

object OzonPriceCheckerApp extends ZIOAppDefault:
  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] = zio.Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    runBot()
      .catchAll(t => ZIO.fail(s"Got error while running ${this.getClass.getSimpleName}: $makeCauseSeqMessage(t)"))
      .provideLayer(AppLayers.ozonPriceCheckerAppLayer)

  private def runBot(): RIO[LongPollingUpdateConsumer with ConsumerRegisterer with AppConfig, Unit] =
    ZIO.scoped {
      for {
        startDateTime   <- getCurrentDateTime
        botsApplication <- BotsApplicationScopes.makeLongPollingBotsApplication()

        longPollingUpdateConsumer <- ZIO.service[LongPollingUpdateConsumer]
        consumerRegisterer        <- ZIO.service[ConsumerRegisterer]
        appConfig                 <- ZIO.service[AppConfig]

        _ <- consumerRegisterer.registerConsumer(botsApplication, longPollingUpdateConsumer, appConfig.botToken.value)

        _ <- Schedulers.scheduleBotStatusLogging(startDateTime, appConfig.logBotStatusInterval)
      } yield ()
    }
