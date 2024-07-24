package ru.ekuzmichev
package store

import common.ProductId
import store.InMemoryProductStore.ProductState
import store.ProductStore.{SourceId, WatchParams}

import zio.{Ref, Task}

class InMemoryProductStore(productWatchesRef: Ref[ProductState]) extends ProductStore:
  override def checkInitialized(sourceId: SourceId): Task[Boolean] =
    productWatchesRef.get.map(_.contains(sourceId))

  override def emptyState(sourceId: SourceId): Task[Unit] =
    productWatchesRef.update(_ + (sourceId -> Map.empty[ProductId, WatchParams]))

  override def clearState(sourceId: SourceId): Task[Unit] =
    productWatchesRef.update(_ - sourceId)

object InMemoryProductStore:
  type ProductState = Map[SourceId, Map[ProductId, WatchParams]]

  object ProductState:
    def empty: ProductState = Map.empty
