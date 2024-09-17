package ru.ekuzmichev
package product

import util.ozon.OzonShortUrlResolverImpl

import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import zio.test.*
import zio.test.Assertion.*

object OzonProductIdParserIntegrationTest extends ZIOSpecDefault:
  def spec: Spec[Any, Throwable] =
    suite("OzonProductIdParser.parse")(
      test("parse Ozon product id from Ozon URL 'shared' from browser") {
        val parser = makeProductIdParser
        parser
          .parse(
            "https://www.ozon.ru/product/kanistra-dlya-smeshivaniya-benzina-i-masla-dde-2-l-1422144661/?__rr=1&from=share_ios&utm_campaign=productpage_link&utm_medium=share_button&utm_source=smm"
          )
          .map { parseResult =>
            assert(parseResult)(isRight(equalTo("kanistra-dlya-smeshivaniya-benzina-i-masla-dde-2-l-1422144661")))
          }
      },
      test("parse take product id from plain non-URL string") {
        val parser = makeProductIdParser
        parser
          .parse(
            "akusticheskaya-gitara-donner-hush-i-silent-guitar-sunburst-6-strunnaya-988766503"
          )
          .map { parseResult =>
            assert(parseResult)(
              isRight(equalTo("akusticheskaya-gitara-donner-hush-i-silent-guitar-sunburst-6-strunnaya-988766503"))
            )
          }
      },
      test("parse Ozon product id from Ozon link URL without query parameters") {
        val parser = makeProductIdParser
        parser
          .parse(
            "https://www.ozon.ru/product/kanistra-dlya-smeshivaniya-benzina-i-masla-dde-2-l-1422144661"
          )
          .map { parseResult =>
            assert(parseResult)(isRight(equalTo("kanistra-dlya-smeshivaniya-benzina-i-masla-dde-2-l-1422144661")))
          }
      },
      test("parse Ozon product id from Ozon short URL") {
        val parser = makeProductIdParser
        parser
          .parse("https://ozon.ru/t/YKknAE4")
          .map { parseResult =>
            assert(parseResult)(
              isRight(
                equalTo(
                  "kovrik-samonaduvayushchiysya-naturehike-yugu-ultralight-automatic-inflatable-cushion-mummy-blue-b-r-1449496528"
                )
              )
            )
          }
      },
      test("parse Ozon product id from Ozon short URL wrapped with text") {
        val parser = makeProductIdParser
        parser
          .parse(
            "Коврик Самонадувающийся Naturehike Yugu Ultralight Automatic Inflatable Cushion Mummy Blue (Б/Р) https://ozon.ru/t/YKknAE4"
          )
          .map { parseResult =>
            assert(parseResult)(
              isRight(
                equalTo(
                  "kovrik-samonaduvayushchiysya-naturehike-yugu-ultralight-automatic-inflatable-cushion-mummy-blue-b-r-1449496528"
                )
              )
            )
          }
      },
      test("fail if link has no path") {
        val parser = makeProductIdParser
        parser
          .parse("https://www.ozon.ru/product/")
          .map { parseResult =>
            assert(parseResult)(isLeft)
          }
      },
      test("fail if link has invalid host") {
        val parser = makeProductIdParser
        parser
          .parse("https://www.vk.ru/product/kanistra-dlya-smeshivaniya-benzina-i-masla-dde-2-l-1422144661")
          .map { parseResult =>
            assert(parseResult)(isLeft)
          }
      }
    )

  private def makeProductIdParser: ProductIdParser =
    new OzonProductIdParser(new OzonShortUrlResolverImpl(new JsoupBrowser()))
