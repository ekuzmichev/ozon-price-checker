package ru.ekuzmichev
package app

import util.lang.Durations.printDuration
import util.lang.JavaTime.toEpochSeconds
import util.zio.ZioClock.getCurrentDateTime

import zio.{Schedule, UIO, ZIO}

import java.time.LocalDateTime
import scala.concurrent.duration.Duration

object Schedulers:
  def scheduleBotStatusLogging(
      startDateTime: LocalDateTime,
      logBotStatusInterval: Duration
  ): UIO[Unit] =
    getCurrentDateTime
      .flatMap { currentDateTime =>
        def calculateDurationInSecs: Long = toEpochSeconds(currentDateTime) - toEpochSeconds(startDateTime)

        ZIO.log(s"Bot is running already ${printDuration(calculateDurationInSecs)}")
      }
      .schedule(Schedule.fixed(zio.Duration.fromScala(logBotStatusInterval)))
      .unit
