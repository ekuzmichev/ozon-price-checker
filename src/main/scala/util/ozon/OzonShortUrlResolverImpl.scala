package ru.ekuzmichev
package util.ozon

import util.ozon.OzonShortUrlResolverImpl.ProductUrlExtractor

import net.ruippeixotog.scalascraper.browser.Browser
import net.ruippeixotog.scalascraper.dsl.DSL.*
import net.ruippeixotog.scalascraper.dsl.DSL.Extract.*
import net.ruippeixotog.scalascraper.model.Element
import net.ruippeixotog.scalascraper.scraper.HtmlExtractor
import zio.{Task, ZIO}

class OzonShortUrlResolverImpl(browser: Browser) extends OzonShortUrlResolver:
  private type BrowserDocument = browser.DocumentType
    
  override def resolveShortUrl(url: String): Task[String] =
    ZIO.attempt {
      val responseHtml: BrowserDocument = browser.get(url)
      responseHtml >> ProductUrlExtractor
    }

object OzonShortUrlResolverImpl:
  val ProductUrlExtractor: HtmlExtractor[Element, String] = attr("content")("meta[data-hid=property::og:url]")
