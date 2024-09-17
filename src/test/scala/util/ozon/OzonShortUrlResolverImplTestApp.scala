package ru.ekuzmichev
package util.ozon

import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import zio.{Scope, ZIO, ZIOAppArgs, ZIOAppDefault}

object OzonShortUrlResolverImplTestApp extends ZIOAppDefault:
  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    val productShortUrl      = "https://ozon.ru/t/YKknAE4"
    val ozonShortUrlResolver = new OzonShortUrlResolverImpl(new JsoupBrowser())
    ozonShortUrlResolver
      .resolveShortUrl(productShortUrl)
      .tap(productFullUrl => ZIO.log(s"Resolved $productShortUrl into $productFullUrl"))