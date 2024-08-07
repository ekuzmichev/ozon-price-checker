package ru.ekuzmichev
package encryption

import zio.logging.backend.SLF4J
import zio.{Console, Scope, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer}

object EncryptionTestApp extends ZIOAppDefault:
  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] = zio.Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    (for
      text          <- Console.readLine("Text to encrypt: ")
      encDec        <- ZIO.service[EncDec]
      encryptedText <- encDec.encrypt(text)
      _             <- ZIO.log(s"Encrypted text: $encryptedText")
    yield ()).provideLayer(
      ZLayer.fromZIO(Console.readLine("Encryption password: ")).flatMap(env => EncDecLayers.aes256(env.get))
    )
