package ru.ekuzmichev
package config

import zio.{TaskLayer, ULayer, ZLayer}

object AppConfigLayers:
  val impl: TaskLayer[AppConfig] =
    AppConfigProviderLayers.impl.flatMap(env => ZLayer.fromZIO(env.get.provideAppConfig()))
