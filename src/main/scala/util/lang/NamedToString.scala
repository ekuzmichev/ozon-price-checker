package ru.ekuzmichev
package util.lang

trait NamedToString:
  this: Product =>

  override def toString: String =
    s"{ ${this.getClass.getSimpleName}: ${(0 until productArity)
        .map(i => s"${productElementName(i)}: ${productElement(i)}")
        .mkString("{ ", ", ", " }")} }"
