package ru.ekuzmichev
package config

import common.Sensitive
import util.lang.NamedToString

import scala.concurrent.duration.Duration

case class AppConfig(
    botToken: Sensitive[String],
    priceCheckingCron: String,
    logBotStatusInterval: Duration,
    cacheStateFilePath: String,
    admins: Seq[Sensitive[String]]
) extends NamedToString
