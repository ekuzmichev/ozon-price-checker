package ru.ekuzmichev
package config
import common.Sensitive
import encryption.EncDec

import zio.Task

class DecryptingAppConfigProvider(decoratee: AppConfigProvider, encDec: EncDec) extends AppConfigProvider:
  override def provideAppConfig(): Task[AppConfig] =
    decoratee
      .provideAppConfig()
      .flatMap { appConfig =>
        decrypt(appConfig.botToken)
          .map(decryptedBotToken => appConfig.copy(botToken = decryptedBotToken))
      }

  private def decrypt(sensitive: Sensitive[String]): Task[Sensitive[String]] =
    encDec.decrypt(sensitive.value).map(Sensitive.apply)
