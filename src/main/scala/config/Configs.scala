package ru.ekuzmichev
package config

import lang.NamedToString

case class Sensitive[T](value: T):
  override def toString: String = s"***"

case class BotConfig(token: Sensitive[String]) extends NamedToString

case class BotsConfig(ozonPriceChecker: BotConfig) extends NamedToString

case class TelegramConfig(bots: BotsConfig) extends NamedToString

case class AppConfig(telegram: TelegramConfig) extends NamedToString:
  def ozonPriceCheckerBotToken: String = telegram.bots.ozonPriceChecker.token.value
