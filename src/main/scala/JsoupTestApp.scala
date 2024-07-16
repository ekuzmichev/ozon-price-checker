package ru.ekuzmichev

@main
def JsoupTestApp(): Unit = {
  val productId                = "akusticheskaya-gitara-donner-hush-i-silent-guitar-sunburst-6-strunnaya-988766503"
  val ozonPriceFetcher         = new OzonProductFetcher()
  val productInfo: ProductInfo = ozonPriceFetcher.fetchProductInfo(productId)

  println(s"Fetched product: $productInfo")
}
