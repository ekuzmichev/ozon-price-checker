package ru.ekuzmichev
package util.zio

import util.lang.Throwables
import util.lang.Throwables.makeCauseSeqMessage

import zio.ZIO

object ZioLoggingImplicits:
  implicit class Ops[R, E, A](effect: ZIO[R, E, A]) extends AnyVal:
    def logged(
        effectDescription: String,
        printResult: A => String = _.toString,
        printError: E => String = {
          case t: Throwable => makeCauseSeqMessage(t, printStackTrace = true)
          case error        => error.toString
        }
    ): ZIO[R, E, A] =
      ZIO
        .log(s"Executing \"$effectDescription\"")
        .zipRight(effect)
        .tapBoth(
          error => ZIO.logError(s"Failed to execute \"$effectDescription\". Cause: ${printError(error)}"),
          result => ZIO.log(s"Executed \"$effectDescription\". Result: ${printResult(result)}")
        )
