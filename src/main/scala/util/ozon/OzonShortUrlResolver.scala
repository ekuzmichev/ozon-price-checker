package ru.ekuzmichev
package util.ozon

import zio.Task

trait OzonShortUrlResolver:
  def resolveShortUrl(url: String): Task[String]
