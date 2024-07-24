package ru.ekuzmichev
package util.lang

import scala.annotation.tailrec
import scala.collection.mutable.ArrayBuffer

object Throwables:
  def makeCauseSeqMessage(t: Throwable, printStackTrace: Boolean = false, abbreviate: Boolean = true): String =
    val rawCauseSeqMessage = causeChainOf(t).mkString(" caused by ")
    val causeSeqMessage    = if (abbreviate) Strings.abbreviate(rawCauseSeqMessage) else rawCauseSeqMessage
    val stackTraceMessage =
      if (printStackTrace)
        "\n\nStack Trace ==> \n\n" +
          t.getStackTrace.mkString("\t", "\n\t", "\t") +
          "\n\n <== End of Stack Trace\n\n"
      else ""
    causeSeqMessage + stackTraceMessage

  private def causeChainOf(t: Throwable): Seq[Throwable] =
    @tailrec
    def loop(t: Throwable, chain: ArrayBuffer[Throwable]): ArrayBuffer[Throwable] =
      val cause = t.getCause
      if (cause == null || chain.contains(cause)) chain
      else loop(cause, chain :+ cause)

    loop(t, ArrayBuffer(t)).toList