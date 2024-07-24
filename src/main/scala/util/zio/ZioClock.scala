package ru.ekuzmichev
package util.zio

import zio.{UIO, ZIO}

import java.time.LocalDateTime

object ZioClock:
  def getCurrentDateTime: UIO[LocalDateTime] = ZIO.clock.flatMap(_.localDateTime)
