package ru.ekuzmichev
package store

import common.{ChatId, ProductId, UserName}
import store.ProductStore.{ProductCandidate, SourceId, SourceState}
import util.lang.NamedToString

import zio.{Fiber, Task}

trait ProductStore:
  def checkInitialized(sourceId: SourceId): Task[Boolean]
  def emptyState(sourceId: SourceId): Task[Unit]
  def clearState(sourceId: SourceId): Task[Unit]
  def readSourceState(sourceId: SourceId): Task[Option[SourceState]]
  def updateProductCandidate(sourceId: SourceId, productCandidate: ProductCandidate): Task[Boolean]
  def resetProductCandidate(sourceId: SourceId): Task[Boolean]
  def addProduct(sourceId: SourceId, product: ProductStore.Product): Task[Boolean]

object ProductStore:
  case class SourceId(userName: UserName, chatId: ChatId) extends NamedToString

  case class Product(id: ProductId, priceThreshold: Int) extends NamedToString

  sealed trait ProductCandidate

  object ProductCandidate:
    case object WaitingProductId                           extends ProductCandidate
    case class WaitingPriceThreshold(productId: ProductId) extends ProductCandidate with NamedToString

  case class SourceState(
      products: Seq[Product],
      maybeProductCandidate: Option[ProductCandidate],
      maybeScheduleFiberRuntime: Option[Fiber.Runtime[Any, Unit]]
  ) extends NamedToString

  object SourceState:
    def empty: SourceState = SourceState(Seq.empty, None, None)
