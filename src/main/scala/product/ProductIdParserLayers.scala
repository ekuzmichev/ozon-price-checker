package ru.ekuzmichev
package product

import zio.{ULayer, ZLayer}

object ProductIdParserLayers:
  val ozon: ULayer[ProductIdParser] = ZLayer.succeed(new OzonProductIdParser)
