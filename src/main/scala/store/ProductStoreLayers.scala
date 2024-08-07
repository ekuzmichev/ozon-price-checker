package ru.ekuzmichev
package store

import store.InMemoryProductStore.ProductState

import zio.{RLayer, Ref, ULayer, ZIO, ZLayer}

object ProductStoreLayers:
  val inMemory: ULayer[ProductStore] = ZLayer.fromZIO {
    for {
      productStateRef <- Ref.make(ProductState.empty)
    } yield new InMemoryProductStore(productStateRef)
  }

  val cachedOverInMemory: RLayer[CacheStateRepository, ProductStore] =
    ZLayer.environment[CacheStateRepository] ++ inMemory >>>
      ZLayer.fromZIO {
        for {
          productStore         <- ZIO.service[ProductStore]
          cacheStateRepository <- ZIO.service[CacheStateRepository]
        } yield new CacheProductStore(productStore, cacheStateRepository)

      }
