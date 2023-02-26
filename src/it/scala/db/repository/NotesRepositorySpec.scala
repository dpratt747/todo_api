package db.repository

import config.ApplicationConfiguration
import db.context.PostgresZioJdbcContextLayer
import db.migrations.{FlywayMigrations, FlywayMigrationsAlg}
import io.getquill.jdbczio.Quill.DataSource
import pureconfig.ConfigSource
import zio._
import zio.test._
import zio.test.TestAspect._

object NotesRepositorySpec extends ZIOSpecDefault {
  def spec = suite("NotesRepository")(
    test("should insert a note into the notes table") {
      (for {
        repo <- ZIO.service[NotesRepositoryAlg]
        result <- repo.insertNotesTable("test note")
      } yield assertTrue(result == 1L))
        .provide(
          NotesRepository.live,
          PostgresZioJdbcContextLayer.live,
          DataSource.fromPrefix("ctx")
        )
    },
    test("should retrieve a note by its noteID") {
      (for {
        repo <- ZIO.service[NotesRepositoryAlg]
        note = "test note"
        noteID <- repo.insertNotesTable(note)
        result <- repo.getNoteByNoteID(noteID)
      } yield assertTrue(result.exists(_.note == note)))
        .provide(
          NotesRepository.live,
          PostgresZioJdbcContextLayer.live,
          DataSource.fromPrefix("ctx")
        )
    },
    test("should fail to retrieve a note by its noteID") {
      (for {
        repo <- ZIO.service[NotesRepositoryAlg]
        result <- repo.getNoteByNoteID(20L)
      } yield assertTrue(result.isEmpty))
        .provide(
          NotesRepository.live,
          PostgresZioJdbcContextLayer.live,
          DataSource.fromPrefix("ctx")
        )
    },
    test("should get all the notes that are stored") {
      (for {
        repo <- ZIO.service[NotesRepositoryAlg]
        _ <- repo.insertNotesTable("test note 1")
        _ <- repo.insertNotesTable("test note 2")
        _ <- repo.insertNotesTable("test note 3")
        result <- repo.getAllNotes
      } yield assertTrue(result.length == 3))
        .provide(
          NotesRepository.live,
          PostgresZioJdbcContextLayer.live,
          DataSource.fromPrefix("ctx")
        )
    }
  ) @@ before(
    ZIO
      .serviceWithZIO[FlywayMigrationsAlg](_.migrate)
      .provide(FlywayMigrations.live, ApplicationConfiguration.live(ConfigSource.default))
  ) @@ after(
    ZIO
      .serviceWithZIO[FlywayMigrationsAlg](_.clean)
      .provide(FlywayMigrations.live, ApplicationConfiguration.live(ConfigSource.default)).ignore
  ) @@ sequential

}
