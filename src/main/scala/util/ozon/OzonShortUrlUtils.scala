package ru.ekuzmichev
package util.ozon

object OzonShortUrlUtils:
  def isShortUrl(url: String): Boolean = ".*ozon.ru/t/.*".r.matches(url)
