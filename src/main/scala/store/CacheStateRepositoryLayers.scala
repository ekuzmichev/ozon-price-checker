package ru.ekuzmichev
package store

import config.AppConfig

import better.files.File as ScalaFile
import zio.{RLayer, ZIO, ZLayer}

object CacheStateRepositoryLayers:
  val file: RLayer[AppConfig, FileCacheStateRepository] = ZLayer.fromZIO {
    for
      appConfig <- ZIO.service[AppConfig]
      cacheStateFilePath = appConfig.cacheStateFilePath
      file <- ZIO.attempt(ScalaFile(cacheStateFilePath))
      _    <- ZIO.log(s"Recreating file $file if not exists")
      _    <- ZIO.attempt(file.createIfNotExists(createParents = true))
    yield new FileCacheStateRepository(cacheStateFilePath)
  }
