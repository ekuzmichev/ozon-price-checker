package ru.ekuzmichev
package app

import bot.OzonPriceCheckerBot
import lang.Throwables.makeCauseSeqMessage
import store.InMemoryProductStore
import store.InMemoryProductStore.ProductState

import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.longpolling.{BotSession, TelegramBotsLongPollingApplication}
import zio.{Ref, Task, ZIO}

object OzonPriceCheckerBotRegisterer:
  def registerOzonPriceCheckerBot(
      botsApplication: TelegramBotsLongPollingApplication,
      token: String
  ): Task[BotSession] =
    for {
      runtime         <- ZIO.runtime[Any]
      telegramClient  <- ZIO.attempt(new OkHttpTelegramClient(token))
      productStateRef <- Ref.make(ProductState.empty)
      productStore = new InMemoryProductStore(productStateRef)
      ozonPriceCheckerBot <- ZIO.attempt(new OzonPriceCheckerBot(telegramClient, productStore, runtime))
      botSession <- ZIO
        .attempt(
          botsApplication.registerBot(token, ozonPriceCheckerBot)
        )
        .tapBoth(
          t =>
            ZIO
              .logError(s"Failed to register OzonPriceCheckerBot: ${makeCauseSeqMessage(t, printStackTrace = true)}"),
          botSession =>
            ZIO.log(
              s"Registered bot '${ozonPriceCheckerBot.getClass.getSimpleName}'. Session is running: ${botSession.isRunning}"
            )
        )
    } yield botSession
