package ru.ekuzmichev
package consumer

import product.{ProductFetcher, ProductIdParser}
import schedule.ZioScheduler
import store.ProductStore

import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer
import org.telegram.telegrambots.meta.generics.TelegramClient
import zio.{RLayer, ZIO, ZLayer}

object UpdateConsumerLayers:
  val ozonPriceChecker: RLayer[
    TelegramClient & ProductStore & ProductFetcher & ProductWatchingJobScheduler & ProductIdParser & CommandProcessor,
    LongPollingUpdateConsumer
  ] =
    ZLayer.fromZIO {
      for
        telegramClient              <- ZIO.service[TelegramClient]
        productStore                <- ZIO.service[ProductStore]
        productFetcher              <- ZIO.service[ProductFetcher]
        productWatchingJobScheduler <- ZIO.service[ProductWatchingJobScheduler]
        productIdParser             <- ZIO.service[ProductIdParser]
        commandProcessor            <- ZIO.service[CommandProcessor]
        runtime                     <- ZIO.runtime[Any]
      yield new OzonPriceCheckerUpdateConsumer(
        telegramClient,
        productStore,
        productFetcher,
        productIdParser,
        commandProcessor,
        productWatchingJobScheduler,
        runtime
      )
    }
