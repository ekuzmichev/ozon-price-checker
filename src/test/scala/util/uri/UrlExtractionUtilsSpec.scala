package ru.ekuzmichev
package util.uri

import io.lemonlabs.uri.{QueryString, Url}
import zio.test.{Spec, ZIOSpecDefault, assertTrue}

object UrlExtractionUtilsSpec extends ZIOSpecDefault:
  def spec: Spec[Any, Throwable] =
    suite("UrlExtractionUtilsSpec.extractUrl")(
      test("should extract URL from text containing it") {
        for maybeUrl <- UrlExtractionUtils.extractUrl(
            s"Коврик Самонадувающийся Naturehike Yugu Ultralight Automatic Inflatable Cushion Mummy Blue (Б/Р) https://ozon.ru/t/YKknAE4"
          )
        yield assertTrue(maybeUrl.contains(Url.apply(scheme = "https", host = "ozon.ru", path = "/t/YKknAE4")))
      },
      test("should extract URL from URL as is")(
        for maybeUrl <- UrlExtractionUtils.extractUrl(
            s"https://www.ozon.ru/product/leska-dlya-trimmera-2-4-mm-denzel-extra-cord-vitoy-kvadrat-243-m-dvuhkomponentnaya-iz-poliamida-932200216"
          )
        yield assertTrue(
          maybeUrl.contains(
            Url.apply(
              scheme = "https",
              host = "www.ozon.ru",
              path =
                "/product/leska-dlya-trimmera-2-4-mm-denzel-extra-cord-vitoy-kvadrat-243-m-dvuhkomponentnaya-iz-poliamida-932200216",
            )
          )
        )
      ),
      test("should extract nothing from text that does not contain any URL") {
        for maybeUrl <- UrlExtractionUtils.extractUrl(
            s"Коврик Самонадувающийся Naturehike Yugu Ultralight"
          )
        yield assertTrue(maybeUrl.isEmpty)
      }
    )
