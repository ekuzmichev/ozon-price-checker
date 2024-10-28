package ru.ekuzmichev
package consumer

import config.AppConfig
import store.ProductStore

import org.telegram.telegrambots.meta.generics.TelegramClient
import zio.{RLayer, ZIO, ZLayer}

object CommandProcessorLayers:
  val ozonPriceChecker: RLayer[ProductStore & TelegramClient & AppConfig, CommandProcessor] =
    ZLayer.fromZIO(for
      productStore   <- ZIO.service[ProductStore]
      telegramClient <- ZIO.service[TelegramClient]
      appConfig      <- ZIO.service[AppConfig]
    yield new BotCommandProcessor(productStore, telegramClient, appConfig.admins.map(_.value)))
