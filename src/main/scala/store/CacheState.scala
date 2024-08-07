package ru.ekuzmichev
package store

import common.{ChatId, UserName}
import store.ProductStore.Product
import util.lang.NamedToString

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class CacheStateEntry(userName: UserName, chatId: ChatId, products: Seq[Product]) extends NamedToString

case class CacheState(entries: Seq[CacheStateEntry]) extends NamedToString:
  def nonEmpty: Boolean = entries.nonEmpty
  def size: Int         = entries.size

object CacheState:
  def empty: CacheState = CacheState(Seq.empty)

  implicit val cacheStateCodec: Codec[CacheState] =
    import io.circe.generic.auto.*
    deriveCodec[CacheState]
