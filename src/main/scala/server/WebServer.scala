package server

import config.ApplicationConfiguration
import db.context.PostgresZioJdbcContextLayer
import db.migrations.{FlywayMigrations, FlywayMigrationsAlg}
import db.persistence.NotesTagsPersistence
import db.repository._
import http.{NoteRoutes, NoteRoutesAlg}
import io.getquill.jdbczio.Quill.DataSource
import program.{CreateNoteProgram, GetNoteProgram}
import pureconfig.ConfigSource
import zhttp.service.Server
import zio._

object WebServer extends ZIOAppDefault {
  override def run: ZIO[Any, Object, Unit] =
    (for {
      config <- ZIO.service[ApplicationConfiguration.Configuration]
      _ <- ZIO.serviceWithZIO[FlywayMigrationsAlg](_.migrate)
      noteRoutes <- ZIO.serviceWith[NoteRoutesAlg](_.routes)
      _ <- ZIO.logInfo(s"Server started on port ${config.server.port}")
      _ <- Server.start(config.server.port, noteRoutes)
    } yield ()).tapErrorCause(cause => ZIO.logErrorCause("Failed to start the server", cause))
      .provide(
        ApplicationConfiguration.live(ConfigSource.default),
        NoteRoutes.live,
        CreateNoteProgram.live,
        GetNoteProgram.live,
        NotesTagsPersistence.live,
        NotesRepository.live,
        TagsRepository.live,
        NotesTagsRepository.live,
        FlywayMigrations.live,
        PostgresZioJdbcContextLayer.live,
        DataSource.fromPrefix("ctx")
      )
}
