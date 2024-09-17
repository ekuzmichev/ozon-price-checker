package ru.ekuzmichev
package util.ozon

import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers.*

class OzonShortUrlUtilsTest extends AnyFlatSpecLike:

  "isShortUrl" should "return true if the format matches .*ozon.ru/t/.*" in {
    OzonShortUrlUtils.isShortUrl("https://ozon.ru/t/YKknAE4") should be(true)
    OzonShortUrlUtils.isShortUrl("https://another-site.ru/t/YKknAE4") should be(false)
    OzonShortUrlUtils.isShortUrl(
      "https://www.ozon.ru/product/leska-dlya-trimmera-2-4-mm-denzel-extra-cord-vitoy-kvadrat-243-m-dvuhkomponentnaya-iz-poliamida-932200216"
    ) should be(false)
  }
