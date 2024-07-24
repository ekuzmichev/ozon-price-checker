package ru.ekuzmichev
package product

import net.ruippeixotog.scalascraper.browser.JsoupBrowser

@main
def OzonProductFetcherTestApp(): Unit = {
  val productId                = "akusticheskaya-gitara-donner-hush-i-silent-guitar-sunburst-6-strunnaya-988766503"
  val priceFetcher             = new OzonProductFetcher(new JsoupBrowser())
  val productInfo: ProductInfo = priceFetcher.fetchProductInfo(productId)

  println(s"Fetched product: $productInfo")
}
