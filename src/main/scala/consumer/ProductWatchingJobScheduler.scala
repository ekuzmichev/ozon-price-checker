package ru.ekuzmichev
package consumer

import store.ProductStore.SourceId

import zio.Task

trait ProductWatchingJobScheduler:
  def scheduleProductsWatching(sourceId: SourceId): Task[Unit]
