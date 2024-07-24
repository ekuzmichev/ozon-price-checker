package ru.ekuzmichev
package schedule

import zio.{RLayer, ULayer, ZLayer}

object ZioSchedulerLayers:
  val impl: RLayer[JobIdGenerator, ZioScheduler] = ZLayer.fromFunction(new ZioSchedulerImpl(_))
