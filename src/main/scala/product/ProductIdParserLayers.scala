package ru.ekuzmichev
package product

import util.ozon.OzonShortUrlResolver

import zio.{RLayer, ZLayer}

object ProductIdParserLayers:
  val ozon: RLayer[OzonShortUrlResolver, ProductIdParser] = ZLayer.fromFunction(new OzonProductIdParser(_))
