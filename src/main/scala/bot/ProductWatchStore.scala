package ru.ekuzmichev
package bot

import common.TypeAliases.{ChatId, ProductId, UserName}

case class SourceId(userName: UserName, chatId: ChatId)

sealed trait WatchParams

object WatchParams:
  case object Initializing                  extends WatchParams
  case class Fulfilled(priceThreshold: Int) extends WatchParams

type ProductWatchStore = Map[SourceId, Map[ProductId, WatchParams]]

object ProductWatchStore:
  def empty: ProductWatchStore = Map.empty
