package ru.ekuzmichev
package lang

import scala.annotation.tailrec
import scala.collection.mutable.ArrayBuffer

object Throwables {
  def causeChainOf(t: Throwable): Seq[Throwable] =
    @tailrec
    def loop(t: Throwable, chain: ArrayBuffer[Throwable]): ArrayBuffer[Throwable] =
      val cause = t.getCause
      if (cause == null || chain.contains(cause)) chain
      else loop(cause, chain :+ cause)

    loop(t, ArrayBuffer(t)).toList

  def makeErrorCauseMessage(t: Throwable, printStackTrace: Boolean = false): String =
    causeChainOf(t).mkString(" caused by ") + {
      if (printStackTrace)
        "\n\nStack Trace ==> \n\n" +
          t.getStackTrace.mkString("\t", "\n\t", "\t") +
          "\n\n <== End of Stack Trace\n\n"
      else ""
    }

}
