package ru.ekuzmichev
package store

import store.ProductStore.Product

import com.stephenn.scalatest.circe.JsonMatchers
import io.circe.*
import io.circe.parser.*
import io.circe.syntax.*
import org.scalatest.Inside.inside
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers.*

class CacheStateTest extends AnyFlatSpecLike with JsonMatchers:
  it should "encode/decode CacheState" in {
    val cacheState: CacheState = CacheState(
      Seq(
        CacheStateEntry(
          userName = "bob",
          chatId = "123",
          products = Seq(
            Product(id = "pencil-1", priceThreshold = 10),
            Product(id = "battery-20", priceThreshold = 200)
          )
        ),
        CacheStateEntry(
          userName = "mike",
          chatId = "456",
          products = Seq(
            Product(id = "pen-10", priceThreshold = 30),
            Product(id = "box-42", priceThreshold = 400)
          )
        )
      )
    )

    val json = cacheState.asJson.noSpaces
    json should matchJson(
      """
        |{
        |  "entries": [
        |    {
        |      "userName": "bob",
        |      "chatId": "123",
        |      "products": [
        |        {
        |          "id": "pencil-1",
        |          "priceThreshold": 10
        |        },
        |        {
        |          "id": "battery-20",
        |          "priceThreshold": 200
        |        }
        |      ]
        |    },
        |    {
        |      "userName": "mike",
        |      "chatId": "456",
        |      "products": [
        |        {
        |          "id": "pen-10",
        |          "priceThreshold": 30
        |        },
        |        {
        |          "id": "box-42",
        |          "priceThreshold": 400
        |        }
        |      ]
        |    }
        |  ]
        |}
        |""".stripMargin
    )
    println(json)

    val decodedCacheStateResult: Either[Error, CacheState] = decode[CacheState](json)
    inside(decodedCacheStateResult) { case Right(decodedCacheState) =>
      decodedCacheState should be(cacheState)
    }
    println(decodedCacheStateResult)
  }
