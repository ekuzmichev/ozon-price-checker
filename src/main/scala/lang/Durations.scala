package ru.ekuzmichev
package lang

object Durations:
  def printDuration(seconds: Long): String =
    f"${seconds / 3600}:${(seconds % 3600) / 60}%02d:${seconds % 60}%02d"
