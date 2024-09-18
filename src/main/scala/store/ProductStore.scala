package ru.ekuzmichev
package store

import common.{ChatId, ProductId, UserName}
import store.ProductStore.{ProductCandidate, SourceId, SourceState}
import util.lang.NamedToString

import zio.Task

trait ProductStore:
  def preInitialize(sourceStatesBySourceId: Map[SourceId, SourceState]): Task[Unit]
  def checkInitialized(sourceId: SourceId): Task[Boolean]
  def checkHasProductId(sourceId: SourceId, productId: ProductId): Task[Boolean]
  def emptyState(sourceId: SourceId): Task[Unit]
  def clearState(sourceId: SourceId): Task[Unit]
  def readSourceState(sourceId: SourceId): Task[Option[SourceState]]
  def readAll(): Task[Map[SourceId, SourceState]]
  def updateProductCandidate(sourceId: SourceId, productCandidate: ProductCandidate): Task[Boolean]
  def resetProductCandidate(sourceId: SourceId): Task[Boolean]
  def addProduct(sourceId: SourceId, product: ProductStore.Product): Task[Boolean]
  def removeProduct(sourceId: SourceId, productId: ProductId): Task[Boolean]
  def removeProduct(sourceId: SourceId, productIndex: Int): Task[Boolean]

object ProductStore:
  case class SourceId(userName: UserName, chatId: ChatId) extends NamedToString

  case class Product(id: ProductId, priceThreshold: Int) extends NamedToString

  sealed trait ProductCandidate

  object ProductCandidate:
    case object WaitingProductId extends ProductCandidate
    case class WaitingPriceThreshold(productId: ProductId, productPrice: Double)
        extends ProductCandidate
        with NamedToString

  case class SourceState(products: Seq[Product], maybeProductCandidate: Option[ProductCandidate]) extends NamedToString

  object SourceState:
    def empty: SourceState = SourceState(Seq.empty, None)
