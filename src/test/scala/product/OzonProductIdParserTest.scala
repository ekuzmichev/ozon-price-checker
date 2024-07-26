package ru.ekuzmichev
package product

import org.scalatest.Inside.*
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers.*

class OzonProductIdParserTest extends AnyFlatSpecLike:

  it should "parse Ozon product id from Ozon URL 'shared' from browser" in {
    val parser = new OzonProductIdParser
    val parseResult = parser.parse(
      "https://www.ozon.ru/product/kanistra-dlya-smeshivaniya-benzina-i-masla-dde-2-l-1422144661/?__rr=1&from=share_ios&utm_campaign=productpage_link&utm_medium=share_button&utm_source=smm"
    )

    inside(parseResult) { case Right(productId) =>
      productId should be("kanistra-dlya-smeshivaniya-benzina-i-masla-dde-2-l-1422144661")
    }
  }

  it should "parse take product id from plain non-URL string" in {
    val parser = new OzonProductIdParser
    val parseResult = parser.parse(
      "akusticheskaya-gitara-donner-hush-i-silent-guitar-sunburst-6-strunnaya-988766503"
    )

    inside(parseResult) { case Right(productId) =>
      productId should be("akusticheskaya-gitara-donner-hush-i-silent-guitar-sunburst-6-strunnaya-988766503")
    }
  }

  it should "parse Ozon product id from Ozon link URL without query parameters" in {
    val parser = new OzonProductIdParser
    val parseResult = parser.parse(
      "https://www.ozon.ru/product/kanistra-dlya-smeshivaniya-benzina-i-masla-dde-2-l-1422144661"
    )

    inside(parseResult) { case Right(productId) =>
      productId should be("kanistra-dlya-smeshivaniya-benzina-i-masla-dde-2-l-1422144661")
    }
  }

  it should "fail if link has no path" in {
    val parser      = new OzonProductIdParser
    val parseResult = parser.parse("https://www.ozon.ru/product/")

    parseResult.isLeft should be(true)
  }

  it should "fail if link has invalid host" in {
    val parser = new OzonProductIdParser
    val parseResult =
      parser.parse("https://www.vk.ru/product/kanistra-dlya-smeshivaniya-benzina-i-masla-dde-2-l-1422144661")

    parseResult.isLeft should be(true)
  }
