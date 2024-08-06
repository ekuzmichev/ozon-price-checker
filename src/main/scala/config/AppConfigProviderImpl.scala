package ru.ekuzmichev
package config

import config.AppConfigProviderImpl.makeAppConfigZioConfig
import config.TypeSafeConfigProviders.makeFromResourceFile
import encryption.EncDec

import zio.Config.*
import zio.config.*
import zio.config.magnolia.*
import zio.{Config, Task}

class AppConfigProviderImpl(encDec: EncDec) extends AppConfigProvider:
  override def provideAppConfig(): Task[AppConfig] =
    makeConfigProvider()
      .flatMap(_.load(makeAppConfigZioConfig(encDec)))
      .flatMap(rawAppConfig =>
        encDec
          .decrypt(rawAppConfig.botToken.value)
          .map(decryptedBotTokenValue => rawAppConfig.copy(botToken = Sensitive.apply(decryptedBotTokenValue)))
      )

  private def makeConfigProvider() =
    for {
      baseConfigProvider  <- TypeSafeConfigProviders.makeFromResourceFile("app.conf")
      localConfigProvider <- TypeSafeConfigProviders.makeFromResourceFile("app.local.conf")
    } yield baseConfigProvider.orElse(localConfigProvider)

object AppConfigProviderImpl:
  private def makeAppConfigZioConfig(encDec: EncDec): Config[AppConfig] =
    implicit val sensitiveDeriveConfig: DeriveConfig[Sensitive] =
      DeriveConfig[String].map(Sensitive.apply)

    deriveConfig[AppConfig].toKebabCase
