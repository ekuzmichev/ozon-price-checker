package ru.ekuzmichev
package util.ozon

import io.lemonlabs.uri.Url
import zio.Task

trait OzonShortUrlResolver:
  def resolveShortUrl(url: Url): Task[Url]
