package ru.ekuzmichev
package config

import util.lang.NamedToString

case class Sensitive(value: String):
  override def toString: String = s"***"

case class AppConfig(botToken: Sensitive, priceCheckingCron: String) extends NamedToString
