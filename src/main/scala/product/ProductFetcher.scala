package ru.ekuzmichev
package product

import common.ProductId

trait ProductFetcher:
  def fetchProductInfo(productId: ProductId): ProductInfo
