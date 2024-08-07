package ru.ekuzmichev
package config

import encryption.EncDec

import zio.{RLayer, ZIO, ZLayer}

object AppConfigLayers:
  val impl: RLayer[EncDec, AppConfig] =
    AppConfigProviderLayers.impl.flatMap(env =>
      ZLayer.fromZIO(env.get.provideAppConfig().tap(appConfig => ZIO.log(s"Parsed: $appConfig")))
    )
