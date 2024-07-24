package ru.ekuzmichev
package telegram

import config.AppConfig

import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.meta.generics.TelegramClient
import zio.{RLayer, ZIO, ZLayer}

object TelegramClientLayers:
  val okHttp: RLayer[AppConfig, TelegramClient] = ZLayer.fromZIO {
    for {
      appConfig <- ZIO.service[AppConfig]
    } yield new OkHttpTelegramClient(appConfig.botToken.value)
  }
