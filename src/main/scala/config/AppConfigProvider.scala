package ru.ekuzmichev
package config

import config.TypeSafeConfigProviders.makeFromResourceFile

import zio.Config.*
import zio.config.*
import zio.config.magnolia.*
import zio.{Config, Task}

object AppConfigProvider {
  def provideAppConfig(): Task[AppConfig] =
    makeConfigProvider().flatMap(_.load(appConfigZioConfig))

  private implicit def sensitiveDeriveConfig[T: DeriveConfig]: DeriveConfig[Sensitive[T]] =
    DeriveConfig[T].map(Sensitive.apply)

  private lazy val appConfigZioConfig: Config[AppConfig] = deriveConfig[AppConfig].toKebabCase

  private def makeConfigProvider() =
    for {
      baseConfigProvider  <- TypeSafeConfigProviders.makeFromResourceFile("app.conf")
      localConfigProvider <- TypeSafeConfigProviders.makeFromResourceFile("app.local.conf")
    } yield baseConfigProvider.orElse(localConfigProvider)
}
