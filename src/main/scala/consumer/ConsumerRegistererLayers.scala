package ru.ekuzmichev
package consumer

import zio.{ULayer, ZLayer}

object ConsumerRegistererLayers:
  val impl: ULayer[ConsumerRegisterer] = ZLayer.succeed(new ConsumerRegistererImpl)
