package ru.ekuzmichev
package store
import util.lang.Throwables.failure

import better.files.{File as ScalaFile, *}
import io.circe.parser.decode
import io.circe.syntax.*
import zio.{Task, ZIO}

class FileCacheStateRepository(filePath: String) extends CacheStateRepository:
  override def read(): Task[CacheState] =
    ZIO
      .attempt(asScalaFile)
      .flatMap(file =>
        if file.exists then
          ZIO
            .attempt(file.contentAsString)
            .flatMap(json =>
              if json.isBlank then ZIO.succeed(CacheState.empty)
              else
                ZIO
                  .fromEither(decode[CacheState](json))
                  .mapError(error => failure(s"Failed to read cache state: $error"))
            )
        else ZIO.succeed(CacheState.empty)
      )

  override def replace(cacheState: CacheState): Task[Unit] =
    ZIO.attempt(asScalaFile.overwrite(cacheState.asJson.spaces2))

  private def asScalaFile: ScalaFile = ScalaFile(filePath)
