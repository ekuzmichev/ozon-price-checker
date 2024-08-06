package ru.ekuzmichev
package store

import common.ProductId
import store.InMemoryProductStore.ProductState
import store.ProductStore.{Product, ProductCandidate, SourceId, SourceState}

import zio.{Fiber, Ref, Task, ZIO}

class InMemoryProductStore(productStateRef: Ref[ProductState]) extends ProductStore:
  override def checkInitialized(sourceId: SourceId): Task[Boolean] =
    productStateRef.get.map(_.contains(sourceId))

  override def emptyState(sourceId: SourceId): Task[Unit] =
    getAllFiberRuntimes(sourceId).flatMap(fiberRuntimes =>
      ZIO.foreachDiscard(fiberRuntimes)(fiberRuntime =>
        fiberRuntime.interrupt.tap(exit =>
          ZIO.log(s"Fiber ${fiberRuntime.id} has been interrupted with exit code $exit")
        )
      )
    ) *>
      productStateRef.update(_ + (sourceId -> SourceState.empty))

  private def getAllFiberRuntimes(sourceId: SourceId): Task[Seq[Fiber.Runtime[Any, Unit]]] =
    productStateRef.get
      .map(_.get(sourceId).collect { case SourceState(_, _, Some(fiberRuntime)) => fiberRuntime })
      .map(_.fold(Seq.empty)(Seq.apply(_)))

  override def clearState(sourceId: SourceId): Task[Unit] =
    productStateRef.update(_ - sourceId)

  override def readSourceState(sourceId: SourceId): Task[Option[SourceState]] =
    productStateRef.get.map(_.get(sourceId))

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

  override def addScheduleFiberRuntime(sourceId: SourceId, fiberRuntime: Fiber.Runtime[Any, Unit]): Task[Boolean] =
    doUpdate(sourceId, sourceState => sourceState.copy(maybeScheduleFiberRuntime = Some(fiberRuntime)))

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
          productStateRef.update(_ + (sourceId -> newSourceState)).as(true)
        case None => ZIO.succeed(false)
    } yield updated

object InMemoryProductStore:
  type ProductState = Map[SourceId, SourceState]

  object ProductState:
    def empty: ProductState = Map.empty
