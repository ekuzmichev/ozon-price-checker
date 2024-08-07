package ru.ekuzmichev
package config

import util.lang.NamedToString

import scala.concurrent.duration.Duration

case class Sensitive(value: String):
  override def toString: String = s"***"

case class AppConfig(
    botToken: Sensitive,
    priceCheckingCron: String,
    logBotStatusInterval: Duration,
    cacheStateFilePath: String
) extends NamedToString
