package ru.ekuzmichev
package encryption

import zio.logging.backend.SLF4J
import zio.{Console, Scope, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer}

object DecryptionTestApp extends ZIOAppDefault:
  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] = zio.Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    (for {
      text          <- Console.readLine("Text to decrypt: ")
      encDec        <- ZIO.service[EncDec]
      decryptedText <- encDec.decrypt(text)
      _             <- ZIO.log(s"Decrypted text: $decryptedText")
    } yield ()).provideLayer(EncDecLayers.aes256)
