package ru.ekuzmichev
package app

import lang.Throwables.makeCauseSeqMessage

import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication
import zio.{RIO, Scope, UIO, ZIO}

object BotsApplicationCreator:
  def createBotsApplication(): RIO[Scope, TelegramBotsLongPollingApplication] =
    ZIO.acquireRelease {
      ZIO
        .attempt(new TelegramBotsLongPollingApplication)
        .tapBoth(
          t => ZIO.logError(s"Failed to start TelegramBotsLongPollingApplication: ${makeCauseSeqMessage(t)}"),
          botsApplication => ZIO.log(s"Bots application is running: ${botsApplication.isRunning}")
        )
    } { botsApplication =>
      close(botsApplication) *> stop(botsApplication)
    }

  private def close(botsApplication: TelegramBotsLongPollingApplication): UIO[Unit] =
    ZIO
      .log(s"Closing bots application")
      .zipRight(ZIO.when(botsApplication.isRunning)(ZIO.attempt(botsApplication.close())))
      .tapBoth(
        error => ZIO.log(s"Closing bots application...FAILED. Cause: $error"),
        _ => ZIO.log(s"Closing bots application...DONE. App is running: ${botsApplication.isRunning}")
      )
      .ignore

  private def stop(botsApplication: TelegramBotsLongPollingApplication): UIO[Unit] =
    ZIO
      .log(s"Stopping bots application")
      .zipRight(ZIO.when(botsApplication.isRunning)(ZIO.attempt(botsApplication.stop())))
      .tapBoth(
        error => ZIO.log(s"Stopping bots application...FAILED. Cause: $error"),
        _ => ZIO.log(s"Stopping bots application...DONE. App is running: ${botsApplication.isRunning}")
      )
      .ignore
