package ru.ekuzmichev
package store

import common.TypeAliases.{ChatId, UserName}
import store.ProductStore.SourceId

import zio.Task

trait ProductStore:
  def checkInitialized(sourceId: SourceId): Task[Boolean]
  def emptyState(sourceId: SourceId): Task[Unit]
  def clearState(sourceId: SourceId): Task[Unit]

object ProductStore:
  case class SourceId(userName: UserName, chatId: ChatId)

  sealed trait WatchParams

  object WatchParams:
    case object Initializing                  extends WatchParams
    case class Fulfilled(priceThreshold: Int) extends WatchParams
