package ru.ekuzmichev
package product

import common.ProductId

import zio.Task

trait ProductIdParser:
  def parse(s: String): Task[Either[String, ProductId]]
