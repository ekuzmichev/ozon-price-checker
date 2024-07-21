package ru.ekuzmichev
package app

import bot.OzonPriceCheckerBot
import config.{AppConfig, AppConfigProvider}

import org.telegram.telegrambots.longpolling.{BotSession, TelegramBotsLongPollingApplication}
import zio.logging.backend.SLF4J
import zio.{RIO, Schedule, Scope, Task, UIO, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer, durationInt}

import java.time.{LocalDateTime, ZoneId}

object OzonPriceCheckerApp extends ZIOAppDefault {
  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] = zio.Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    for {
      appConfig <- parseAppConfig()
      _         <- runBots(appConfig)
    } yield ()

  private def runBots(appConfig: AppConfig) =
    ZIO.scoped {
      for {
        startDateTime   <- getCurrentDateTime()
        botsApplication <- createBotsApplication()
        _               <- registerOzonPriceCheckerBot(botsApplication, appConfig.ozonPriceCheckerBotToken)
        _               <- scheduleRunningStatusLog(startDateTime)
      } yield ()
    }

  private def scheduleRunningStatusLog(startDateTime: LocalDateTime) =
    getCurrentDateTime()
      .flatMap(currentDateTime =>
        ZIO.log(
          s"Bots are running already " +
            s"${printDuration(toEpochSeconds(currentDateTime) - toEpochSeconds(startDateTime))}"
        )
      )
      .schedule(Schedule.fixed(10.hours))

  private def toEpochSeconds(localDateTime: LocalDateTime): Long =
    localDateTime.atZone(ZoneId.systemDefault()).toEpochSecond

  private def getCurrentDateTime(): UIO[LocalDateTime] = ZIO.clock.flatMap(_.localDateTime)

  private def printDuration(seconds: Long) =
    f"${seconds / 3600}:${(seconds % 3600) / 60}%02d:${seconds % 60}%02d"

  private def parseAppConfig(): Task[AppConfig] =
    AppConfigProvider
      .provideAppConfig()
      .tap(appConfig => ZIO.log(s"Parsed app configuration: $appConfig"))

  private def createBotsApplication(): RIO[Scope, TelegramBotsLongPollingApplication] =
    ZIO.acquireRelease {
      ZIO
        .attempt(new TelegramBotsLongPollingApplication)
        .tap(botsApplication => ZIO.log(s"Bots application is running: ${botsApplication.isRunning}"))
    } { botsApplication =>
      close(botsApplication) *> stop(botsApplication)
    }

  private def close(botsApplication: TelegramBotsLongPollingApplication): UIO[Unit] =
    ZIO
      .log(s"Closing bots application")
      .zipRight(ZIO.when(botsApplication.isRunning)(ZIO.attempt(botsApplication.close())))
      .tapBoth(
        error => ZIO.log(s"Closing bots application...FAILED. Cause: $error"),
        _ => ZIO.log(s"Closing bots application...DONE. App is running: ${botsApplication.isRunning}")
      )
      .ignore

  private def stop(botsApplication: TelegramBotsLongPollingApplication): UIO[Unit] =
    ZIO
      .log(s"Stopping bots application")
      .zipRight(ZIO.when(botsApplication.isRunning)(ZIO.attempt(botsApplication.stop())))
      .tapBoth(
        error => ZIO.log(s"Stopping bots application...FAILED. Cause: $error"),
        _ => ZIO.log(s"Stopping bots application...DONE. App is running: ${botsApplication.isRunning}")
      )
      .ignore

  private def registerOzonPriceCheckerBot(
      botsApplication: TelegramBotsLongPollingApplication,
      token: String
  ): Task[BotSession] =
    for {
      ozonPriceCheckerBot <- ZIO.attempt(new OzonPriceCheckerBot())
      botSession <- ZIO.attempt(
        botsApplication.registerBot(token, ozonPriceCheckerBot)
      )
      _ <- ZIO.log(
        s"Registered bot '${ozonPriceCheckerBot.getClass.getSimpleName}'. Session is running: ${botSession.isRunning}"
      )
    } yield botSession
}
