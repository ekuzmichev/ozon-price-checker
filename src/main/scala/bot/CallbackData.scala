package ru.ekuzmichev
package bot

import io.circe.Decoder.Result
import io.circe.{Codec, DecodingFailure, HCursor, Json}

sealed trait CallbackData

object CallbackData:
  case class DeleteProduct(index: Int) extends CallbackData

  // TODO: Migrate to circe-generic-extras instead of manual codec when it is available for Scala 3
  implicit val codec: Codec[CallbackData] = new Codec[CallbackData]:
    override def apply(hCursor: HCursor): Result[CallbackData] =
      hCursor.downField("type").as[String].flatMap {
        case "DeleteProduct" =>
          hCursor.downField("index").as[Int].map(DeleteProduct.apply)
        case unknown =>
          Left(DecodingFailure(s"Unknown type $unknown", hCursor.history))
      }

    override def apply(callbackData: CallbackData): Json = callbackData match
      case DeleteProduct(index) =>
        Json.obj(("type", Json.fromString("DeleteProduct")), ("index", Json.fromInt(index)))
