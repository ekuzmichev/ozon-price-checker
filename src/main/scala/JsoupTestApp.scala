package ru.ekuzmichev

import net.ruippeixotog.scalascraper.browser.{Browser, JsoupBrowser}
import net.ruippeixotog.scalascraper.dsl.DSL.*
import net.ruippeixotog.scalascraper.dsl.DSL.Extract.*
import net.ruippeixotog.scalascraper.dsl.DSL.Parse.*
import net.ruippeixotog.scalascraper.model.*
import net.ruippeixotog.scalascraper.scraper.HtmlExtractor

@main
def JsoupTestApp(): Unit = {
  val baseUrl    = "https://www.ozon.ru"
  val productId  = "akusticheskaya-gitara-donner-hush-i-silent-guitar-sunburst-6-strunnaya-988766503"
  val productUrl = s"$baseUrl/product/$productId"

  val browser: Browser = JsoupBrowser()

  val responseHtml: browser.DocumentType = browser.get(productUrl)

  val rawPriceAsStringExtractor: HtmlExtractor[Element, String] =
    extractor("div[data-widget=webPrice] span", text, asIs[String])

  val price: Int =
    (responseHtml >> rawPriceAsStringExtractor)
      .trim()
      .replace(" ", "")
      .replace("₽", "")
      .toInt

  println(s"The price is $price ₽")
}
