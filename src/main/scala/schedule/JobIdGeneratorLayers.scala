package ru.ekuzmichev
package schedule

import zio.{ULayer, ZLayer}

object JobIdGeneratorLayers:
  val alphaNumeric: ULayer[JobIdGenerator] = ZLayer.succeed(new AlphaNumericJobIdGenerator)
