package ru.ekuzmichev
package schedule

import zio.UIO

trait JobIdGenerator:
  def generateJobId(): UIO[String]
