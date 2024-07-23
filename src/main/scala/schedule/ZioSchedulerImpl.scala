package ru.ekuzmichev
package schedule
import schedule.ZioSchedulerImpl.{TimeProviderExhausted, TimeProviderVoid}

import zio.*

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import scala.util.control.NoStackTrace

class ZioSchedulerImpl(jobIdGenerator: JobIdGenerator) extends ZioScheduler:
  override def schedule[R, E, A](
      effect: ZIO[R, E, A],
      nextTimeProvider: NextTimeProvider,
      label: String
  ): ZIO[R, Any, Unit] =
    jobIdGenerator.generateJobId().flatMap { jobId =>
      ZIO.logAnnotate("jobLabel", makeJobLabel(label, jobId)) {
        doSchedule(effect, nextTimeProvider)
          .catchSome { case TimeProviderExhausted =>
            ZIO.logWarning(s"Time provider ${nextTimeProvider.info} has been exhausted")
          }
      }
    }

  private def makeJobLabel[A, E, R](label: String, jobId: String) =
    if (label.trim.isEmpty) s"$jobId" else s"$label|$jobId"

  private def doSchedule[R, E, A](
      effect: ZIO[R, E, A],
      nextTimeProvider: NextTimeProvider
  ): ZIO[R, Any, Unit] =
    for {
      jobRunNumberRef <- Ref.make(0)
      _               <- ZIO.logDebug(s"Scheduling job by time provider ${nextTimeProvider.info}")
      _               <- runAtNextTime(effect, nextTimeProvider, jobRunNumberRef).repeat(Schedule.forever)
    } yield ()

  private def runAtNextTime[R, E, A](
      effect: ZIO[R, E, A],
      nextTimeProvider: NextTimeProvider,
      jobRunNumberRef: Ref[Int]
  ): ZIO[R, Any, Unit] =
    jobRunNumberRef.get.flatMap { jobRunNumber =>
      ZIO.logAnnotate("jobRunNumber", jobRunNumber.toString) {
        for {
          sleepDuration <- calculateSleepDuration(nextTimeProvider, jobRunNumber)
          _             <- ZIO.log(s"Sleeping ${sleepDuration.render} before the job run")
          _             <- runDelayed(effect, sleepDuration)
          _             <- jobRunNumberRef.update(_ + 1)
        } yield ()
      }
    }

  private def calculateSleepDuration(nextTimeProvider: NextTimeProvider, jobRunNumber: Int): Task[Duration] =
    for {
      now <- getNow
      next <- ZIO
        .fromOption(nextTimeProvider.nexDateTime(now))
        .orElseFail(if (jobRunNumber == 0) TimeProviderVoid else TimeProviderExhausted)
      _ <- ZIO.log(s"Got next run time: $next")
    } yield Duration.fromNanos(now.until(next, ChronoUnit.NANOS))

  private def getNow: UIO[LocalDateTime] = ZIO.clock.flatMap(_.localDateTime)

  private def runDelayed[R, E, A](effect: ZIO[R, E, A], sleepDuration: Duration): ZIO[R, E, Unit] =
    val io =
      getNow.flatMap(now => ZIO.log(s"Running job at $now")) *>
        effect
          .tapBoth(
            error => getNow.flatMap(now => ZIO.logError(s"Job failed at $now with error: $error")),
            res => getNow.flatMap(now => ZIO.logDebug(s"Job finished ar $now with result: $res"))
          )
          .ignore
    if (sleepDuration.isZero) io else io.delay(sleepDuration)

object ZioSchedulerImpl:
  case object TimeProviderVoid      extends RuntimeException with NoStackTrace
  case object TimeProviderExhausted extends RuntimeException with NoStackTrace
