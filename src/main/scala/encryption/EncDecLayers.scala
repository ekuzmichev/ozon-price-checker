package ru.ekuzmichev
package encryption

import org.jasypt.util.text.AES256TextEncryptor
import zio.{Console, TaskLayer, ZIO, ZLayer}

object EncDecLayers:
  val aes256: TaskLayer[EncDec] = ZLayer.fromZIO(
    for {
      encryptionPassword <- Console.readLine("Encryption password: ")
      textEncryptor      <- ZIO.attempt(new AES256TextEncryptor())
      _                  <- ZIO.attempt(textEncryptor.setPassword(encryptionPassword))
    } yield new JasyptEncDec(textEncryptor)
  )
