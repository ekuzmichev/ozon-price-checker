package ru.ekuzmichev
package schedule

import cats.syntax.either.*
import cron4s.lib.javatime.*
import cron4s.syntax.cron.*
import cron4s.{Cron, CronExpr}

import java.time.LocalDateTime
import scala.util.Try

class Cron4sNextTimeProvider(cron: CronExpr) extends NextTimeProvider:
  override def nexDateTime(from: LocalDateTime): Option[LocalDateTime] =
    cron.next[LocalDateTime](from)

  override def info: String = s"cron=$cron"

object Cron4sNextTimeProvider:
  def fromCronString(cronStr: String): Try[Cron4sNextTimeProvider] =
    Cron(cronStr.toLowerCase)
      .leftMap(error => new RuntimeException(s"Failed to build cron from $cronStr. Cause: $error"))
      .map(cronExpr => new Cron4sNextTimeProvider(cronExpr))
      .toTry
