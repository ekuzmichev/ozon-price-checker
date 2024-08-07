package ru.ekuzmichev
package product

import common.ProductId

import zio.Task

trait ProductFetcher:
  def fetchProductInfo(productId: ProductId): Task[Option[ProductInfo]]
