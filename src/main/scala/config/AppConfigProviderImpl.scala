package ru.ekuzmichev
package config

import config.AppConfigProviderImpl.makeAppConfigZioConfig
import config.TypeSafeConfigProviders.makeFromResourceFile
import encryption.EncDec

import cron4s.Cron
import zio.Config.*
import zio.config.*
import zio.config.magnolia.*
import zio.{Config, IO, Task, ZIO}

import scala.concurrent.duration.Duration

class AppConfigProviderImpl(encDec: EncDec) extends AppConfigProvider:
  override def provideAppConfig(): Task[AppConfig] =
    makeConfigProvider()
      .flatMap(_.load(makeAppConfigZioConfig(encDec)))
      .flatMap(rawAppConfig =>
        encDec
          .decrypt(rawAppConfig.botToken.value)
          .map(decryptedBotTokenValue => rawAppConfig.copy(botToken = Sensitive.apply(decryptedBotTokenValue)))
      )
      .tap(appConfig => validateCron(appConfig.priceCheckingCron))

  private def validateCron(cron: String): IO[cron4s.Error, Unit] =
    ZIO.fromEither(Cron(cron)).tapError(error => ZIO.logError(s"Failed to validate cron $cron: $error")).unit

  private def makeConfigProvider() =
    for {
      baseConfigProvider  <- TypeSafeConfigProviders.makeFromResourceFile("app.conf")
      localConfigProvider <- TypeSafeConfigProviders.makeFromResourceFile("app.local.conf")
    } yield baseConfigProvider.orElse(localConfigProvider)

object AppConfigProviderImpl:
  private def makeAppConfigZioConfig(encDec: EncDec): Config[AppConfig] =
    implicit val sensitiveDeriveConfig: DeriveConfig[Sensitive] =
      DeriveConfig[String].map(Sensitive.apply)
    
    implicit val durationDeriveConfig: DeriveConfig[Duration] =
      DeriveConfig[String].map(Duration.apply)

    deriveConfig[AppConfig].toKebabCase
