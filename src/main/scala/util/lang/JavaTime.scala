package ru.ekuzmichev
package util.lang

import java.time.{LocalDateTime, ZoneId}

object JavaTime:
  def toEpochSeconds(localDateTime: LocalDateTime): Long =
    localDateTime.atZone(ZoneId.systemDefault()).toEpochSecond
