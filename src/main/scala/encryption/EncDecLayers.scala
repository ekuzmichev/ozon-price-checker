package ru.ekuzmichev
package encryption

import org.jasypt.util.text.AES256TextEncryptor
import zio.{TaskLayer, ZIO, ZLayer}

object EncDecLayers:
  def aes256(encryptionPassword: String): TaskLayer[EncDec] = ZLayer.fromZIO(
    for
      textEncryptor <- ZIO.attempt(new AES256TextEncryptor())
      _             <- ZIO.attempt(textEncryptor.setPassword(encryptionPassword))
    yield new JasyptEncDec(textEncryptor)
  )
