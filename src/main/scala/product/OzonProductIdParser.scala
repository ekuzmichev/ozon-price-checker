package ru.ekuzmichev
package product

import common.ProductId
import product.OzonProductIdParser.OzonHostRegex
import util.lang.Throwables
import util.lang.Throwables.makeCauseSeqMessage

import cats.syntax.either.*
import io.lemonlabs.uri.{Url, UrlPath}

import scala.util.matching.Regex

class OzonProductIdParser extends ProductIdParser:
  override def parse(s: String): Either[String, ProductId] =
    parseUrlEither(s).flatMap(extractProductId(s, _))

  private def parseUrlEither(s: String): Either[String, Url] =
    Url.parseTry(s).toEither.leftMap(makeCauseSeqMessage(_))

  private def extractProductId(s: ProductId, url: Url): Either[String, ProductId] =
    if (isOzonUrl(url))
      takeProductIdFromPath(url.path) match
        case Some(productId) => Right(productId)
        case None            => Left(s"Not found product ID in URL path. URL: $url")
    else if (url.hostOption.nonEmpty)
      Left("URL has host other than *ozon.ru")
    else
      Right(s)

  private def isOzonUrl(url: Url): Boolean =
    url.hostOption.map(_.value).exists(OzonHostRegex.matches)

  private def takeProductIdFromPath(urlPath: UrlPath): Option[ProductId] =
    urlPath.parts.drop(1).filter(!_.isBlank).headOption

object OzonProductIdParser:
  val OzonHostRegex: Regex = ".*ozon.ru".r
