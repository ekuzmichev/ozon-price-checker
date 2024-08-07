package ru.ekuzmichev
package store

import common.{ChatId, UserName}
import store.ProductStore.Product

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

case class CacheStateEntry(userName: UserName, chatId: ChatId, products: Seq[Product])

case class CacheState(entries: Seq[CacheStateEntry])

object CacheState:
  implicit val cacheStateCodec: Codec[CacheState] = {
    import io.circe.generic.auto.*
    deriveCodec[CacheState]
  }
