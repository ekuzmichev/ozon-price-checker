package ru.ekuzmichev
package config

import encryption.EncDec

import zio.{RLayer, ULayer, ZLayer}

object AppConfigProviderLayers:
  val impl: ULayer[AppConfigProvider] = ZLayer.succeed(new AppConfigProviderImpl)

  val decryptingOverImpl: RLayer[EncDec, AppConfigProvider] =
    ZLayer.environment[EncDec] ++ impl >>>
      ZLayer.fromFunction(new DecryptingAppConfigProvider(_, _))
