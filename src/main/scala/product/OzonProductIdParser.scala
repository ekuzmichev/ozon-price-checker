package ru.ekuzmichev
package product

import common.ProductId
import product.OzonProductIdParser.{OzonHostRegex, OzonProductUrlPathPart, OzonShortUrlPathPart}
import util.ozon.OzonShortUrlResolver
import util.uri.UrlExtractionUtils

import io.lemonlabs.uri.{Url, UrlPath}
import zio.{Task, ZIO}

import scala.util.matching.Regex

class OzonProductIdParser(ozonShortUrlResolver: OzonShortUrlResolver) extends ProductIdParser:
  override def parse(s: String): Task[Either[String, ProductId]] =
    UrlExtractionUtils.extractUrl(s).flatMap {
      case Some(url) =>
        if isOzonUrl(url) then
          if isShortOzonUrl(url) then
            ozonShortUrlResolver
              .resolveShortUrl(url)
              .flatMap(extractProductId)
          else extractProductId(url)
        else ZIO.left(s"URL $url contains host other than *ozon.ru")
      case None =>
        ZIO.logDebug(s"Returning $s as product id") *> ZIO.right(s)
    }

  private def extractProductId(url: Url) =
    if isProductUrl(url) then
      takeProductIdFromPath(url.path) match
        case Some(productId) => ZIO.right(productId)
        case None            => ZIO.left(s"Not found product ID in URL path. URL: $url")
    else ZIO.left(s"URL $url is not product OZON URL")

  private def isShortOzonUrl(url: Url): Boolean = url.path.parts.contains(OzonShortUrlPathPart)

  private def isProductUrl(url: Url) = url.path.parts.contains(OzonProductUrlPathPart)

  private def extractProductId(s: ProductId, url: Url): Either[String, ProductId] =
    if isOzonUrl(url) then
      takeProductIdFromPath(url.path) match
        case Some(productId) => Right(productId)
        case None            => Left(s"Not found product ID in URL path. URL: $url")
    else if url.hostOption.nonEmpty then Left("URL has host other than *ozon.ru")
    else Right(s)

  private def isOzonUrl(url: Url): Boolean =
    url.hostOption.map(_.value).exists(OzonHostRegex.matches)

  private def takeProductIdFromPath(urlPath: UrlPath): Option[ProductId] =
    urlPath.parts.drop(1).filter(!_.isBlank).headOption

object OzonProductIdParser:
  val OzonHostRegex: Regex           = ".*ozon.ru".r
  val OzonShortUrlPathPart: String   = "t"
  val OzonProductUrlPathPart: String = "product"
