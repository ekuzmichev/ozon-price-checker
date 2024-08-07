package ru.ekuzmichev
package store

import config.AppConfig
import encryption.EncDec

import better.files.File as ScalaFile
import zio.{RLayer, ZIO, ZLayer}

object CacheStateRepositoryLayers:
  val file: RLayer[AppConfig & EncDec, FileCacheStateRepository] = ZLayer.fromZIO {
    for
      appConfig <- ZIO.service[AppConfig]
      encDec    <- ZIO.service[EncDec]
      cacheStateFilePath = appConfig.cacheStateFilePath
      file <- ZIO.attempt(ScalaFile(cacheStateFilePath))
      _    <- ZIO.log(s"Recreating file $file if not exists")
      _    <- ZIO.attempt(file.createIfNotExists(createParents = true))
    yield new FileCacheStateRepository(cacheStateFilePath, encDec)
  }
