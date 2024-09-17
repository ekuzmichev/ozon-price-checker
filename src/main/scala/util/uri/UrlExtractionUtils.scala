package ru.ekuzmichev
package util.uri

import util.lang.Throwables.makeCauseSeqMessage

import io.lemonlabs.uri.Url
import zio.{Task, ZIO}

import scala.util.Failure
import scala.util.matching.compat.Regex

object UrlExtractionUtils:
  private val StringContainingUrlRegex: Regex = s".*((?:https|http)://\\S*\\.*\\S+\\.[a-zA-Z]{2,5}\\S*).*".r

  def extractUrl(s: String): Task[Option[Url]] =
    s match
      case StringContainingUrlRegex(url) =>
        ZIO
          .attempt(Url.parseTry(url))
          .tapSome { case Failure(t) =>
            ZIO.log(s"Failed to parse $url as URL: ${makeCauseSeqMessage(t)}")
          }
          .map(_.toOption)
      case _ => ZIO.log(s"Not found any url in text $s").as(None)
