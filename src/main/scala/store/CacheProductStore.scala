package ru.ekuzmichev
package store
import common.ProductId
import store.ProductStore.{Product, ProductCandidate, SourceId, SourceState}

import zio.Task

class CacheProductStore(decoratee: ProductStore, cacheStateRepository: CacheStateRepository) extends ProductStore:
  override def preInitialize(sourceStatesBySourceId: Map[SourceId, SourceState]): Task[Unit] =
    decoratee.preInitialize(sourceStatesBySourceId)

  override def checkInitialized(sourceId: SourceId): Task[Boolean] =
    decoratee.checkInitialized(sourceId)

  override def checkHasProductId(sourceId: SourceId, productId: ProductId): Task[Boolean] =
    decoratee.checkHasProductId(sourceId, productId)

  override def emptyState(sourceId: SourceId): Task[Unit] =
    decoratee.emptyState(sourceId) <* replaceStateInCache()

  private def replaceStateInCache(): Task[Unit] =
    readAsCacheState().tap(cacheStateRepository.replace).unit

  private def readAsCacheState(): Task[CacheState] =
    readAll().map(toCacheState)

  private def toCacheState(sourceStatesBySourceId: Map[SourceId, SourceState]): CacheState =
    CacheState(
      entries = sourceStatesBySourceId.map { case (sourceId, sourceState) =>
        CacheStateEntry(
          userName = sourceId.userName,
          chatId = sourceId.chatId,
          products = sourceState.products
        )
      }.toSeq
    )

  override def clearState(sourceId: SourceId): Task[Unit] =
    decoratee.clearState(sourceId) <* replaceStateInCache()

  override def readSourceState(sourceId: SourceId): Task[Option[SourceState]] =
    decoratee.readSourceState(sourceId)

  override def readAll(): Task[Map[SourceId, SourceState]] =
    decoratee.readAll()

  override def updateProductCandidate(
      sourceId: SourceId,
      productCandidate: ProductCandidate
  ): Task[Boolean] =
    decoratee.updateProductCandidate(sourceId, productCandidate)

  override def resetProductCandidate(sourceId: SourceId): Task[Boolean] =
    decoratee.resetProductCandidate(sourceId)

  override def addProduct(sourceId: SourceId, product: Product): Task[Boolean] =
    decoratee.addProduct(sourceId, product) <* replaceStateInCache()
