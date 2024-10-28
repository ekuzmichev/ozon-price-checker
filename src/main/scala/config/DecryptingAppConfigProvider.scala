package ru.ekuzmichev
package config
import common.Sensitive
import encryption.EncDec

import zio.{Task, ZIO}

class DecryptingAppConfigProvider(decoratee: AppConfigProvider, encDec: EncDec) extends AppConfigProvider:
  override def provideAppConfig(): Task[AppConfig] =
    decoratee
      .provideAppConfig()
      .flatMap { appConfig =>
        for
          decryptedBotToken <- decrypt(appConfig.botToken)
          decryptedAdmins   <- decrypt(appConfig.admins)
        yield appConfig.copy(botToken = decryptedBotToken)
      }

  private def decrypt(sensitive: Sensitive[String]): Task[Sensitive[String]] =
    encDec.decrypt(sensitive.value).map(Sensitive.apply)

  private def decrypt(sensitives: Seq[Sensitive[String]]): Task[Seq[Sensitive[String]]] =
    ZIO.foreach(sensitives)(decrypt)
