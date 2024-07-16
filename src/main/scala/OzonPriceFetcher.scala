package ru.ekuzmichev

import OzonPriceFetcher.{OzonBaseUrl, RawPriceAsStringExtractor}

import net.ruippeixotog.scalascraper.browser.{Browser, JsoupBrowser}
import net.ruippeixotog.scalascraper.dsl.DSL.*
import net.ruippeixotog.scalascraper.dsl.DSL.Extract.*
import net.ruippeixotog.scalascraper.dsl.DSL.Parse.*
import net.ruippeixotog.scalascraper.model.*
import net.ruippeixotog.scalascraper.scraper.HtmlExtractor

class OzonPriceFetcher {
  private val browser: Browser = JsoupBrowser()

  def fetchPrice(productId: String): Int = {
    val productUrl: String                 = makeProductUrl(productId)
    val responseHtml: browser.DocumentType = browser.get(productUrl)
    extractPrice(responseHtml)
  }

  private def extractPrice(responseHtml: browser.DocumentType): Int = {
    (responseHtml >> RawPriceAsStringExtractor)
      .trim()
      .replace(" ", "")
      .replace("₽", "")
      .toInt
  }

  private def makeProductUrl(productId: String): String =
    s"$OzonBaseUrl/product/$productId"
}

object OzonPriceFetcher {
  private val OzonBaseUrl: String = "https://www.ozon.ru"
  private val RawPriceAsStringExtractor: HtmlExtractor[Element, String] =
    extractor("div[data-widget=webPrice] span", text, asIs[String])
}
