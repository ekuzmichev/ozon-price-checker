package ru.ekuzmichev
package store

import zio.Task

trait CacheStateRepository:
  def read(): Task[CacheState]
  def replace(cacheState: CacheState): Task[Unit]
