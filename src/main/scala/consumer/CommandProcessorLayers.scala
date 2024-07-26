package ru.ekuzmichev
package consumer

import store.ProductStore

import org.telegram.telegrambots.meta.generics.TelegramClient
import zio.{RLayer, ZLayer}

object CommandProcessorLayers:
  val ozonPriceChecker: RLayer[ProductStore with TelegramClient, CommandProcessor] =
    ZLayer.fromFunction(new OzonPriceCheckerCommandProcessor(_, _))
