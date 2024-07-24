package ru.ekuzmichev
package store

import store.InMemoryProductStore.ProductState

import zio.{Ref, ULayer, ZLayer}

object ProductStoreLayers:
  val inMemory: ULayer[ProductStore] = ZLayer.fromZIO {
    for {
      productStateRef <- Ref.make(ProductState.empty)
    } yield new InMemoryProductStore(productStateRef)
  }
