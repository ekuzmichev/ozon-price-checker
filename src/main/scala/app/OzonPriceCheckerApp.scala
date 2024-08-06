package ru.ekuzmichev
package app

import config.{AppConfig, AppConfigLayers}
import consumer.*
import encryption.EncDecLayers
import product.{ProductFetcherLayers, ProductIdParserLayers}
import scalascraper.BrowserLayers
import schedule.{JobIdGeneratorLayers, ZioSchedulerLayers}
import store.ProductStoreLayers
import telegram.{BotsApplicationScopes, TelegramClientLayers}
import util.lang.Durations.printDuration
import util.lang.JavaTime.toEpochSeconds
import util.lang.Throwables.{failure, makeCauseSeqMessage}
import util.zio.ZioClock.getCurrentDateTime

import org.telegram.telegrambots.longpolling.BotSession
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer
import zio.logging.backend.SLF4J
import zio.{RIO, RLayer, Schedule, Scope, TaskLayer, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer, durationInt}

import java.time.LocalDateTime

object OzonPriceCheckerApp extends ZIOAppDefault:
  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] = zio.Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    runBots()
      .catchAll(t => ZIO.fail(s"Got error while running ${this.getClass.getSimpleName}: $makeCauseSeqMessage(t)"))
      .provideLayer(programLayer)

  private def programLayer: RLayer[ZIOAppArgs, AppConfig with LongPollingUpdateConsumer with ConsumerRegisterer] =
    ZLayer
      .fromZIO(
        getArgs.flatMap(args =>
          args.headOption match
            case Some(encryptionPassword) => ZIO.succeed(encryptionPassword)
            case None                     => ZIO.fail(failure(s"No encryption password provided"))
        )
      )
      .flatMap { encryptionPasswordEnv =>
        ZLayer.make[AppConfig with LongPollingUpdateConsumer with ConsumerRegisterer](
          AppConfigLayers.impl,
          ConsumerRegistererLayers.impl,
          UpdateConsumerLayers.ozonPriceChecker,
          TelegramClientLayers.okHttp,
          ProductStoreLayers.inMemory,
          ProductFetcherLayers.ozon,
          ZioSchedulerLayers.impl,
          BrowserLayers.jsoup,
          JobIdGeneratorLayers.alphaNumeric,
          ProductIdParserLayers.ozon,
          CommandProcessorLayers.ozonPriceChecker,
          EncDecLayers.aes256(encryptionPasswordEnv.get),
          ProductWatchingJobSchedulerLayers.impl
        )
      }

  private def runBots(): RIO[LongPollingUpdateConsumer with ConsumerRegisterer with AppConfig, Unit] =
    ZIO.scoped {
      for {
        startDateTime             <- getCurrentDateTime
        botsApplication           <- BotsApplicationScopes.makeLongPollingBotsApplication()
        longPollingUpdateConsumer <- ZIO.service[LongPollingUpdateConsumer]
        consumerRegisterer        <- ZIO.service[ConsumerRegisterer]
        appConfig                 <- ZIO.service[AppConfig]
        _ <- consumerRegisterer.registerConsumer(botsApplication, longPollingUpdateConsumer, appConfig.botToken.value)
        _ <- scheduleRunningStatusLogging(startDateTime)
      } yield ()
    }

  private def scheduleRunningStatusLogging(startDateTime: LocalDateTime) =
    getCurrentDateTime
      .flatMap(currentDateTime =>
        ZIO.log(
          s"Bots are running already " +
            s"${printDuration(toEpochSeconds(currentDateTime) - toEpochSeconds(startDateTime))}"
        )
      )
      .schedule(Schedule.fixed(10.hours))

end OzonPriceCheckerApp
