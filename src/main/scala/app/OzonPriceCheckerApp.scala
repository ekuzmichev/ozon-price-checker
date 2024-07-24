package ru.ekuzmichev
package app

import app.BotsApplicationCreator.createBotsApplication
import app.OzonPriceCheckerBotRegisterer.registerOzonPriceCheckerBot
import config.{AppConfig, AppConfigProvider}
import lang.Durations.printDuration
import lang.JavaTime.toEpochSeconds
import lang.Throwables.makeCauseSeqMessage
import util.zio.ZioClock.getCurrentDateTime

import org.telegram.telegrambots.longpolling.BotSession
import zio.logging.backend.SLF4J
import zio.{Schedule, Scope, Task, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer, durationInt}

import java.time.LocalDateTime

object OzonPriceCheckerApp extends ZIOAppDefault {
  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] = zio.Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    (for {
      appConfig <- provideAppConfig()
      _         <- runBots(appConfig)
    } yield ())
      .catchAll(t => ZIO.fail(s"Got error while running ${this.getClass.getSimpleName}: $makeCauseSeqMessage(t)"))

  private def provideAppConfig(): Task[AppConfig] =
    AppConfigProvider
      .provideAppConfig()
      .tap(appConfig => ZIO.log(s"Got app configuration: $appConfig"))

  private def runBots(appConfig: AppConfig) =
    ZIO.scoped {
      for {
        startDateTime   <- getCurrentDateTime
        botsApplication <- createBotsApplication()
        _               <- registerOzonPriceCheckerBot(botsApplication, appConfig.ozonPriceCheckerBotToken)
        _               <- scheduleRunningStatusLog(startDateTime)
      } yield ()
    }

  private def scheduleRunningStatusLog(startDateTime: LocalDateTime) =
    getCurrentDateTime
      .flatMap(currentDateTime =>
        ZIO.log(
          s"Bots are running already " +
            s"${printDuration(toEpochSeconds(currentDateTime) - toEpochSeconds(startDateTime))}"
        )
      )
      .schedule(Schedule.fixed(10.hours))

}
