package ru.ekuzmichev
package common

type UserName  = String
type ChatId    = String
type ProductId = String

case class Sensitive[T](value: T):
  override def toString: String = s"***"