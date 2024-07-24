package ru.ekuzmichev
package config

import zio.{ULayer, ZLayer}

object AppConfigProviderLayers:
  val impl: ULayer[AppConfigProvider] = ZLayer.succeed(new AppConfigProviderImpl)
