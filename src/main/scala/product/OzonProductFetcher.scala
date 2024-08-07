package ru.ekuzmichev
package product

import common.ProductId
import product.OzonProductFetcher.{OzonBaseUrl, ProductNameAsStringExtractor, RawPriceAsStringExtractor}

import net.ruippeixotog.scalascraper.browser.Browser
import net.ruippeixotog.scalascraper.dsl.DSL.*
import net.ruippeixotog.scalascraper.dsl.DSL.Extract.*
import net.ruippeixotog.scalascraper.dsl.DSL.Parse.*
import net.ruippeixotog.scalascraper.model.*
import net.ruippeixotog.scalascraper.scraper.HtmlExtractor
import zio.{Task, ZIO}

class OzonProductFetcher(browser: Browser) extends ProductFetcher:
  private type BrowserDocument = browser.DocumentType

  override def fetchProductInfo(productId: ProductId): Task[ProductInfo] =
    ZIO.attempt {
      val productUrl: String            = makeProductUrl(productId)
      val responseHtml: BrowserDocument = browser.get(productUrl)
      val name: String                  = extractProductName(responseHtml)
      val price: Double                 = extractPrice(responseHtml)

      ProductInfo(name, price)
    }

  private def extractProductName(responseHtml: BrowserDocument): String =
    (responseHtml >> ProductNameAsStringExtractor).trim()

  private def extractPrice(responseHtml: BrowserDocument): Double =
    (responseHtml >> RawPriceAsStringExtractor)
      .trim()
      .replace(" ", "")
      .replace("₽", "")
      .replaceAll("[a-zA-Zа-яА-ЯЁё]", "")
      .toDouble

  private def makeProductUrl(productId: String): String =
    s"$OzonBaseUrl/product/$productId"

object OzonProductFetcher:
  private val OzonBaseUrl: String = "https://www.ozon.ru"
  private val RawPriceAsStringExtractor: HtmlExtractor[Element, String] =
    extractor("div[data-widget=webPrice] span", text, asIs[String])
  private val ProductNameAsStringExtractor: HtmlExtractor[Element, String] =
    extractor("div[data-widget=webProductHeading] h1", text, asIs[String])
