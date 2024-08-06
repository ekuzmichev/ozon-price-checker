package ru.ekuzmichev
package encryption
import util.lang.Throwables.{failure, makeCauseSeqMessage}

import org.jasypt.util.text.TextEncryptor
import zio.{Task, ZIO}

class JasyptEncDec(textEncryptor: TextEncryptor) extends EncDec:
  override def encrypt(text: String): Task[String] =
    ZIO
      .attempt(textEncryptor.encrypt(text))
      .catchAll { t =>
        ZIO.fail(failure(s"Failed to encrypt text. Cause: ${makeCauseSeqMessage(t)}"))
      }
  override def decrypt(encryptedText: String): Task[String] =
    ZIO
      .attempt(textEncryptor.decrypt(encryptedText))
      .catchAll { t =>
        ZIO.fail(failure(s"Failed to decrypt text. Cause: ${makeCauseSeqMessage(t)}"))
      }
