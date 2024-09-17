package ru.ekuzmichev
package util.ozon

import util.ozon.OzonShortUrlResolverImpl.ProductUrlExtractor

import io.lemonlabs.uri.Url
import net.ruippeixotog.scalascraper.browser.Browser
import net.ruippeixotog.scalascraper.dsl.DSL.*
import net.ruippeixotog.scalascraper.dsl.DSL.Extract.*
import net.ruippeixotog.scalascraper.model.Element
import net.ruippeixotog.scalascraper.scraper.HtmlExtractor
import zio.{Task, ZIO}

class OzonShortUrlResolverImpl(browser: Browser) extends OzonShortUrlResolver:
  private type BrowserDocument = browser.DocumentType
    
  override def resolveShortUrl(url: Url): Task[Url] =
    ZIO.attempt {
      val responseHtml: BrowserDocument = browser.get(url.toStringPunycode)
      responseHtml >> ProductUrlExtractor
    }.flatMap(fullUrlString => ZIO.fromTry(Url.parseTry(fullUrlString)))
      .tap{fullUrl => ZIO.logDebug(s"Resolved short OZON URL $url into full OZON URL $fullUrl")}

object OzonShortUrlResolverImpl:
  val ProductUrlExtractor: HtmlExtractor[Element, String] = attr("content")("meta[data-hid=property::og:url]")
