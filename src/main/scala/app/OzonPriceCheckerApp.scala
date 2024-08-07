package ru.ekuzmichev
package app

import app.AppLayers.{OzonPriceCheckerAppROut, ozonPriceCheckerAppLayer}
import config.AppConfig
import consumer.*
import store.ProductStore.{SourceId, SourceState}
import store.{CacheState, CacheStateRepository, ProductStore}
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
      .catchAll(t => ZIO.fail(s"Got error while running ${this.getClass.getSimpleName}: ${makeCauseSeqMessage(t)}"))
      .provideLayer(ozonPriceCheckerAppLayer)

  private def runBot(): RIO[OzonPriceCheckerAppROut, Unit] =
    ZIO.scoped {
      for
        startDateTime   <- getCurrentDateTime
        botsApplication <- BotsApplicationScopes.makeLongPollingBotsApplication()

        longPollingUpdateConsumer   <- ZIO.service[LongPollingUpdateConsumer]
        consumerRegisterer          <- ZIO.service[ConsumerRegisterer]
        appConfig                   <- ZIO.service[AppConfig]
        productWatchingJobScheduler <- ZIO.service[ProductWatchingJobScheduler]
        productStore                <- ZIO.service[ProductStore]
        cacheStateRepository        <- ZIO.service[CacheStateRepository]

        _ <- consumerRegisterer.registerConsumer(botsApplication, longPollingUpdateConsumer, appConfig.botToken)

        _ <- ZIO.log(s"Initialized bot messages consumer at $startDateTime")

        _ <- productWatchingJobScheduler.scheduleProductsWatching()

        _ <- ZIO.log(s"Scheduled product watching job")

        cacheState <- cacheStateRepository.read()

        _ <- ZIO.when(cacheState.nonEmpty)(
          productStore
            .preInitialize(toSourceStatesBySourceId(cacheState))
            .zipLeft(ZIO.log(s"Pre-initialized product store with state of size ${cacheState.size}"))
        )

        _ <- Schedulers.scheduleBotStatusLogging(startDateTime, appConfig.logBotStatusInterval)
      yield ()
    }

  private def toSourceStatesBySourceId(cacheState: CacheState): Map[SourceId, SourceState] =
    cacheState.entries.map { entry =>
      val sourceId    = SourceId(entry.userName, entry.chatId)
      val sourceState = SourceState(products = entry.products, None)

      sourceId -> sourceState
    }.toMap
