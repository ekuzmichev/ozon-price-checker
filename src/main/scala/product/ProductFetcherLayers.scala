package ru.ekuzmichev
package product

import net.ruippeixotog.scalascraper.browser.Browser
import zio.{RLayer, ZLayer}

object ProductFetcherLayers:
  val ozon: RLayer[Browser, ProductFetcher] = ZLayer.fromFunction(new OzonProductFetcher(_))
