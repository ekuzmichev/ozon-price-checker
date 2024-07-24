package ru.ekuzmichev
package store

import store.InMemoryProductStore.ProductState
import store.ProductStore.{Product, ProductCandidate, SourceId, SourceState}

import zio.{Ref, Task, ZIO}

class InMemoryProductStore(productWatchesRef: Ref[ProductState]) extends ProductStore:
  override def checkInitialized(sourceId: SourceId): Task[Boolean] =
    productWatchesRef.get.map(_.contains(sourceId))

  override def emptyState(sourceId: SourceId): Task[Unit] =
    productWatchesRef.update(_ + (sourceId -> SourceState.empty))

  override def clearState(sourceId: SourceId): Task[Unit] =
    productWatchesRef.update(_ - sourceId)

  override def readSourceState(sourceId: SourceId): Task[Option[SourceState]] =
    productWatchesRef.get.map(_.get(sourceId))

  override def updateProductCandidate(
      sourceId: SourceId,
      productCandidate: ProductCandidate
  ): Task[Boolean] =
    doUpdateProductCandidate(sourceId, Some(productCandidate))

  override def resetProductCandidate(sourceId: SourceId): Task[Boolean] =
    doUpdateProductCandidate(sourceId, None)

  override def addProduct(sourceId: SourceId, product: ProductStore.Product): Task[Boolean] =
    doUpdate(sourceId, sourceState => sourceState.copy(products = sourceState.products :+ product))
  // TODO: Check if already contains product with id

  private def doUpdateProductCandidate(
      sourceId: SourceId,
      maybeProductCandidate: Option[ProductCandidate]
  ): Task[Boolean] =
    doUpdate(sourceId, _.copy(maybeProductCandidate = maybeProductCandidate))

  private def doUpdate(
      sourceId: SourceId,
      updateSourceStateFn: SourceState => SourceState
  ): Task[Boolean] =
    for {
      maybeSourceState <- readSourceState(sourceId)
      updated <- maybeSourceState match
        case Some(sourceState) =>
          val newSourceState = updateSourceStateFn(sourceState)
          productWatchesRef.update(_ + (sourceId -> newSourceState)).as(true)
        case None => ZIO.succeed(false)
    } yield updated

object InMemoryProductStore:
  type ProductState = Map[SourceId, SourceState]

  object ProductState:
    def empty: ProductState = Map.empty
