package ru.ekuzmichev
package scalascraper

import net.ruippeixotog.scalascraper.browser.{Browser, JsoupBrowser}
import zio.{ULayer, ZLayer}

object BrowserLayers:
  val jsoup: ULayer[Browser] = ZLayer.succeed(new JsoupBrowser())
