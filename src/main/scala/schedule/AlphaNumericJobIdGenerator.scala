package ru.ekuzmichev
package schedule
import zio.{UIO, ZIO}

import scala.util.Random

class AlphaNumericJobIdGenerator extends JobIdGenerator:
  override def generateJobId(): UIO[String] = ZIO.succeed(Random.alphanumeric.take(5).mkString)
