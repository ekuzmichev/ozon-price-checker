package ru.ekuzmichev
package encryption

import zio.Task

trait EncDec:
  def encrypt(text: String): Task[String]
  def decrypt(encryptedText: String): Task[String]
