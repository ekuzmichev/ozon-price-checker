package ru.ekuzmichev
package product

import common.ProductId

trait ProductIdParser:
  def parse(s: String): Either[String, ProductId]
