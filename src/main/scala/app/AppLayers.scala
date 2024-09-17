package ru.ekuzmichev
package app

import app.OzonPriceCheckerApp.getArgs
import common.Sensitive
import config.{AppConfig, AppConfigLayers}
import consumer.*
import encryption.EncDecLayers
import product.{ProductFetcherLayers, ProductIdParserLayers}
import scalascraper.BrowserLayers
import schedule.{JobIdGeneratorLayers, ZioSchedulerLayers}
import store.{CacheStateRepository, CacheStateRepositoryLayers, ProductStore, ProductStoreLayers}
import telegram.TelegramClientLayers
import util.lang.Throwables.failure
import util.ozon.OzonShortUrlResolverLayers

import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer
import zio.{RLayer, Task, TaskLayer, URLayer, ZIO, ZIOAppArgs, ZLayer}

object AppLayers:
  type OzonPriceCheckerAppROut =
    AppConfig & LongPollingUpdateConsumer & ConsumerRegisterer & ProductWatchingJobScheduler & ProductStore &
      CacheStateRepository

  private def parseAppArgs(args: Seq[String]): Task[AppArgs] =
    args.headOption match
      case Some(encryptionPassword) =>
        val appArgs = AppArgs(Sensitive(encryptionPassword))
        ZIO.log(s"Parsed $appArgs").as(appArgs)
      case None =>
        ZIO.fail(failure(s"No encryption password provided"))

  private val appArgsLayer: RLayer[ZIOAppArgs, AppArgs] = ZLayer.fromZIO(getArgs.flatMap(parseAppArgs))

  val ozonPriceCheckerAppLayer: RLayer[ZIOAppArgs, OzonPriceCheckerAppROut] =
    appArgsLayer.flatMap(appArgsEnv => makeOzonPriceCheckerLayer(appArgsEnv.get.encryptionPassword))

  private def makeOzonPriceCheckerLayer(encryptionPassword: Sensitive[String]): TaskLayer[OzonPriceCheckerAppROut] =
    ZLayer.make[OzonPriceCheckerAppROut](
      AppConfigLayers.decryptingOverImpl,
      ConsumerRegistererLayers.impl,
      UpdateConsumerLayers.ozonPriceChecker,
      TelegramClientLayers.okHttp,
      ProductStoreLayers.cachedOverInMemory,
      ProductFetcherLayers.ozon,
      ZioSchedulerLayers.impl,
      BrowserLayers.jsoup,
      JobIdGeneratorLayers.alphaNumeric,
      ProductIdParserLayers.ozon,
      OzonShortUrlResolverLayers.impl,
      CommandProcessorLayers.ozonPriceChecker,
      EncDecLayers.aes256(encryptionPassword.value),
      ProductWatchingJobSchedulerLayers.impl,
      CacheStateRepositoryLayers.file
    )
