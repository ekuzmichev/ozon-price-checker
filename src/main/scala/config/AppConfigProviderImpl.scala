package ru.ekuzmichev
package config

import common.Sensitive
import config.AppConfigProviderImpl.AppConfigZioConfig
import config.TypeSafeConfigProviders.makeFromResourceFile

import zio.Config.*
import zio.config.*
import zio.config.magnolia.*
import zio.{Config, Task}

import scala.concurrent.duration.Duration

class AppConfigProviderImpl extends AppConfigProvider:
  override def provideAppConfig(): Task[AppConfig] =
    makeConfigProvider().flatMap(_.load(AppConfigZioConfig))

  private def makeConfigProvider() =
    for
      baseConfigProvider  <- TypeSafeConfigProviders.makeFromResourceFile("app.conf")
      localConfigProvider <- TypeSafeConfigProviders.makeFromResourceFile("app.local.conf")
    yield baseConfigProvider.orElse(localConfigProvider)

object AppConfigProviderImpl:
  private val AppConfigZioConfig: Config[AppConfig] =
    implicit def sensitiveDeriveConfig[T: DeriveConfig]: DeriveConfig[Sensitive[T]] =
      DeriveConfig[T].map(Sensitive.apply)

    implicit val durationDeriveConfig: DeriveConfig[Duration] =
      DeriveConfig[String].map(Duration.apply)

    deriveConfig[AppConfig].toKebabCase
