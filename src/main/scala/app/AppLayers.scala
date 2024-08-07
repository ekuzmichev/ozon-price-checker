package ru.ekuzmichev
package app

import app.OzonPriceCheckerApp.getArgs
import config.{AppConfig, AppConfigLayers}
import consumer.*
import encryption.EncDecLayers
import product.{ProductFetcherLayers, ProductIdParserLayers}
import scalascraper.BrowserLayers
import schedule.{JobIdGeneratorLayers, ZioSchedulerLayers}
import store.ProductStoreLayers
import telegram.TelegramClientLayers
import util.lang.Throwables.failure

import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer
import zio.{RLayer, ZIO, ZIOAppArgs, ZLayer}

object AppLayers:
  val ozonPriceCheckerAppLayer: RLayer[ZIOAppArgs, AppConfig with LongPollingUpdateConsumer with ConsumerRegisterer] =
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
