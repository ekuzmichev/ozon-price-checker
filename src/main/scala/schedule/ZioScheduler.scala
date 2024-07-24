package ru.ekuzmichev
package schedule

import zio.ZIO

trait ZioScheduler:
  def schedule[R, E, A](effect: ZIO[R, E, A], nextTimeProvider: NextTimeProvider, label: String): ZIO[R, Any, Unit]
