package ru.ekuzmichev
package config

import encryption.EncDec

import zio.{RLayer, ZLayer}

object AppConfigProviderLayers:
  val impl: RLayer[EncDec, AppConfigProvider] = ZLayer.fromFunction(new AppConfigProviderImpl(_))
