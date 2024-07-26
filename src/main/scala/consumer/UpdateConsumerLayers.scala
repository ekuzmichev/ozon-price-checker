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
    TelegramClient with ProductStore with ProductFetcher with ZioScheduler with ProductIdParser with CommandProcessor,
    LongPollingUpdateConsumer
  ] =
    ZLayer.fromZIO {
      for {
        telegramClient   <- ZIO.service[TelegramClient]
        productStore     <- ZIO.service[ProductStore]
        productFetcher   <- ZIO.service[ProductFetcher]
        zioScheduler     <- ZIO.service[ZioScheduler]
        productIdParser  <- ZIO.service[ProductIdParser]
        commandProcessor <- ZIO.service[CommandProcessor]
        runtime          <- ZIO.runtime[Any]

      } yield new OzonPriceCheckerUpdateConsumer(
        telegramClient,
        productStore,
        productFetcher,
        zioScheduler,
        productIdParser,
        commandProcessor,
        runtime
      )
    }
