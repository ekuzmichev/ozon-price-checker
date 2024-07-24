package ru.ekuzmichev
package config

import util.lang.NamedToString

case class Sensitive[T](value: T):
  override def toString: String = s"***"

case class AppConfig(botToken: Sensitive[String], priceCheckingCron: String) extends NamedToString
