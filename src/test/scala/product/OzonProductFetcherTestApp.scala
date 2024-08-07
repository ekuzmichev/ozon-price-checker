package ru.ekuzmichev
package product

import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import zio.logging.backend.SLF4J
import zio.{Scope, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer}

object OzonProductFetcherTestApp extends ZIOAppDefault:
  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] = zio.Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    val productId    = "akusticheskaya-gitara-donner-hush-i-silent-guitar-sunburst-6-strunnaya-988766503"
    val priceFetcher = new OzonProductFetcher(new JsoupBrowser())
    priceFetcher
      .fetchProductInfo(productId)
      .tap(productInfo => ZIO.log(s"Fetched product: $productInfo"))
