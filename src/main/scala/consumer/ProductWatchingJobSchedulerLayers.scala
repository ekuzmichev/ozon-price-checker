package ru.ekuzmichev
package consumer

import config.AppConfig
import product.ProductFetcher
import schedule.ZioScheduler
import store.ProductStore

import org.telegram.telegrambots.meta.generics.TelegramClient
import zio.{Fiber, Ref, URLayer, ZIO, ZLayer}

object ProductWatchingJobSchedulerLayers:
  val impl: URLayer[
    TelegramClient & ProductStore & ProductFetcher & ZioScheduler & AppConfig,
    ProductWatchingJobScheduler
  ] =
    ZLayer.fromZIO(
      for
        telegramClient          <- ZIO.service[TelegramClient]
        productStore            <- ZIO.service[ProductStore]
        productFetcher          <- ZIO.service[ProductFetcher]
        zioScheduler            <- ZIO.service[ZioScheduler]
        appConfig               <- ZIO.service[AppConfig]
        scheduleFiberRuntimeRef <- Ref.make[Option[Fiber.Runtime[Any, Unit]]](None)
      yield new ProductWatchingJobSchedulerImpl(
        telegramClient,
        productStore,
        productFetcher,
        zioScheduler,
        appConfig.priceCheckingCron,
        scheduleFiberRuntimeRef
      )
    )
