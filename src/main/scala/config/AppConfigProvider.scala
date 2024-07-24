package ru.ekuzmichev
package config

import zio.Task

trait AppConfigProvider:
  def provideAppConfig(): Task[AppConfig]
