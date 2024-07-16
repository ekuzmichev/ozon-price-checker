package ru.ekuzmichev

case class ProductInfo(name: String, price: Double):
  override def toString: String = s"{ name: $name, price: $price }"
