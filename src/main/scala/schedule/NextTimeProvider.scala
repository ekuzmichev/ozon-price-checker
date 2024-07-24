package ru.ekuzmichev
package schedule

import java.time.LocalDateTime

trait NextTimeProvider:
  def nexDateTime(from: LocalDateTime): Option[LocalDateTime]
  def info: String = "#"
