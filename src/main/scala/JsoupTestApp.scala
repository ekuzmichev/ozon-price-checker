package ru.ekuzmichev

@main
def JsoupTestApp(): Unit = {
  val productId        = "akusticheskaya-gitara-donner-hush-i-silent-guitar-sunburst-6-strunnaya-988766503"
  val ozonPriceFetcher = new OzonPriceFetcher()
  val price: Int       = ozonPriceFetcher.fetchPrice(productId)

  println(s"The price is $price ₽")
}
