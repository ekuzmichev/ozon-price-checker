package ru.ekuzmichev
package consumer

import zio.Task

trait ProductWatchingJobScheduler:
  def scheduleProductsWatching(): Task[Unit]
