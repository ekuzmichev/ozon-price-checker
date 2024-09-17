package ru.ekuzmichev
package util.ozon

import net.ruippeixotog.scalascraper.browser.Browser
import zio.{URLayer, ZLayer}

object OzonShortUrlResolverLayers:
  val impl: URLayer[Browser, OzonShortUrlResolver] = ZLayer.fromFunction(new OzonShortUrlResolverImpl(_))
