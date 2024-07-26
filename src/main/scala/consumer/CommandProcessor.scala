package ru.ekuzmichev
package consumer

import store.ProductStore.SourceId

import zio.Task

trait CommandProcessor:
  def processCommand(sourceId: SourceId, text: String): Task[Unit]
