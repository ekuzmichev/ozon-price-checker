package ru.ekuzmichev
package schedule

import cron4s.Cron
import zio.logging.backend.SLF4J
import zio.{Scope, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer}

import java.time.LocalDateTime

object TestCron4sZioSchedulerImplApp extends ZIOAppDefault {
  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] = zio.Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    val cron = "0 */1 * * * ?"

    new ZioSchedulerImpl(new AlphaNumericJobIdGenerator())
      .schedule(
        ZIO.log(s"${LocalDateTime.now}: Run"),
        new Cron4sNextRimeProvider(Cron.unsafeParse(cron)),
        "logging-test-job"
      )
}
