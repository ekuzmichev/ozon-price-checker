package ru.ekuzmichev
package lang

object Strings:
  def abbreviate(s: String, maxLength: Int = 1000): String =
    if (s.length <= maxLength) s else s.substring(0, maxLength) + "..."
