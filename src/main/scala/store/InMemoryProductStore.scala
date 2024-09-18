package ru.ekuzmichev
package store

import common.ProductId
import store.InMemoryProductStore.ProductState
import store.ProductStore.{Product, ProductCandidate, SourceId, SourceState}

import zio.{Ref, Task, ZIO}

class InMemoryProductStore(productStateRef: Ref[ProductState]) extends ProductStore:
  override def preInitialize(sourceStatesBySourceId: Map[SourceId, SourceState]): Task[Unit] =
    productStateRef.set(sourceStatesBySourceId)

  override def checkInitialized(sourceId: SourceId): Task[Boolean] =
    productStateRef.get.map(_.contains(sourceId))

  override def emptyState(sourceId: SourceId): Task[Unit] =
    productStateRef.update(_ + (sourceId -> SourceState.empty))

  override def clearState(sourceId: SourceId): Task[Unit] =
    productStateRef.update(_ - sourceId)

  override def readSourceState(sourceId: SourceId): Task[Option[SourceState]] =
    productStateRef.get.map(_.get(sourceId))

  override def readAll(): Task[Map[SourceId, SourceState]] = productStateRef.get

  override def updateProductCandidate(
      sourceId: SourceId,
      productCandidate: ProductCandidate
  ): Task[Boolean] =
    doUpdateProductCandidate(sourceId, Some(productCandidate))

  override def resetProductCandidate(sourceId: SourceId): Task[Boolean] =
    doUpdateProductCandidate(sourceId, None)

  override def addProduct(sourceId: SourceId, product: ProductStore.Product): Task[Boolean] =
    doUpdate(sourceId, sourceState => sourceState.copy(products = sourceState.products :+ product))

  override def checkHasProductId(sourceId: SourceId, productId: ProductId): Task[Boolean] =
    productStateRef.get.map(_.get(sourceId).exists(_.products.exists(_.id == productId)))

  override def removeProduct(sourceId: SourceId, productId: ProductId): Task[Boolean] =
    doUpdate(sourceId, sourceState => sourceState.copy(products = sourceState.products.filter(_.id != productId)))

  override def removeProduct(sourceId: SourceId, productIndex: Int): Task[Boolean] =
    doUpdate(sourceId, sourceState => sourceState.copy(products = sourceState.products.zipWithIndex.filter(_._2 != productIndex).map(_._1)))

  private def doUpdateProductCandidate(
      sourceId: SourceId,
      maybeProductCandidate: Option[ProductCandidate]
  ): Task[Boolean] =
    doUpdate(sourceId, _.copy(maybeProductCandidate = maybeProductCandidate))

  private def doUpdate(
      sourceId: SourceId,
      updateSourceStateFn: SourceState => SourceState
  ): Task[Boolean] =
    for
      maybeSourceState <- readSourceState(sourceId)
      updated <- maybeSourceState match
        case Some(sourceState) =>
          val newSourceState = updateSourceStateFn(sourceState)
          productStateRef.update(_ + (sourceId -> newSourceState)).as(true)
        case None => ZIO.succeed(false)
    yield updated

object InMemoryProductStore:
  type ProductState = Map[SourceId, SourceState]

  object ProductState:
    def empty: ProductState = Map.empty
