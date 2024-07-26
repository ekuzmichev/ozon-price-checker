package ru.ekuzmichev
package util.telegram

import common.ChatId

import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.generics.TelegramClient
import zio.{Task, ZIO}

object MessageSendingUtils:
  def sendTextMessage(chatId: ChatId, message: String)(implicit telegramClient: TelegramClient): Task[Unit] =
    ZIO.attempt {
      val sendMessage = new SendMessage(chatId, message)
      telegramClient.execute(sendMessage)
    }.unit
