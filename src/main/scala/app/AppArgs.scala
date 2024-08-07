package ru.ekuzmichev
package app

import common.Sensitive
import util.lang.NamedToString

case class AppArgs(encryptionPassword: Sensitive[String]) extends NamedToString
