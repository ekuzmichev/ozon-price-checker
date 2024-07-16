package ru.ekuzmichev

import OzonProductFetcher.{OzonBaseUrl, ProductNameAsStringExtractor, RawPriceAsStringExtractor}

import net.ruippeixotog.scalascraper.browser.{Browser, JsoupBrowser}
import net.ruippeixotog.scalascraper.dsl.DSL.*
import net.ruippeixotog.scalascraper.dsl.DSL.Extract.*
import net.ruippeixotog.scalascraper.dsl.DSL.Parse.*
import net.ruippeixotog.scalascraper.model.*
import net.ruippeixotog.scalascraper.scraper.HtmlExtractor

class OzonProductFetcher {
  private val browser: Browser = JsoupBrowser()

  def fetchProductInfo(productId: String): ProductInfo = {
    val productUrl: String                 = makeProductUrl(productId)
    val responseHtml: browser.DocumentType = browser.get(productUrl)
    val name                               = extractProductName(responseHtml)
    val price                              = extractPrice(responseHtml)
    ProductInfo(name, price)
  }

  private def extractProductName(responseHtml: browser.DocumentType): String =
    (responseHtml >> ProductNameAsStringExtractor).trim()

  private def extractPrice(responseHtml: browser.DocumentType): Double = {
    (responseHtml >> RawPriceAsStringExtractor)
      .trim()
      .replace(" ", "")
      .replace("₽", "")
      .toDouble
  }

  private def makeProductUrl(productId: String): String =
    s"$OzonBaseUrl/product/$productId"
}

object OzonProductFetcher {
  private val OzonBaseUrl: String = "https://www.ozon.ru"
  private val RawPriceAsStringExtractor: HtmlExtractor[Element, String] =
    extractor("div[data-widget=webPrice] span", text, asIs[String])
  private val ProductNameAsStringExtractor: HtmlExtractor[Element, String] =
    extractor("div[data-widget=webProductHeading] h1", text, asIs[String])
}
