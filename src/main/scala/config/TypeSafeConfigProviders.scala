package ru.ekuzmichev
package config

import com.typesafe.config.ConfigFactory
import zio.config.*
import zio.config.typesafe.*
import zio.{ConfigProvider, Task}

object TypeSafeConfigProviders:
  def makeFromResourceFile(configResourceFilePath: String): Task[ConfigProvider] =
    ConfigProvider.fromTypesafeConfigZIO(
      ConfigFactory.parseResourcesAnySyntax(configResourceFilePath)
    )
