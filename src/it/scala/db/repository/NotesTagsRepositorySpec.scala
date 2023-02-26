package db.repository

import config.ApplicationConfiguration
import db.context.PostgresZioJdbcContextLayer
import db.migrations.{FlywayMigrations, FlywayMigrationsAlg}
import io.getquill.jdbczio.Quill.DataSource
import pureconfig.ConfigSource
import zio._
import zio.test.TestAspect._
import zio.test._

object NotesTagsRepositorySpec extends ZIOSpecDefault {
  def spec = suite("TagsRepository")(
    test("should insert a row into the note_tags table") {
      (for {
        noteId <- ZIO.serviceWithZIO[NotesRepositoryAlg](
          _.insertNotesTable("Note")
        )
        tagId <- ZIO.serviceWithZIO[TagsRepositoryAlg](_.insertTagsTable("Tag"))
        result <- ZIO.serviceWithZIO[NotesTagsRepositoryAlg](
          _.insertIntoNotesTagsTable(tagId, noteId)
        )
      } yield assertTrue(result == 1))
        .provide(
          NotesRepository.live,
          TagsRepository.live,
          NotesTagsRepository.live,
          PostgresZioJdbcContextLayer.live,
          DataSource.fromPrefix("ctx")
        )
    },
    test("should get all tags by noteID") {
      (for {
        noteId1 <- ZIO.serviceWithZIO[NotesRepositoryAlg](
          _.insertNotesTable("Note")
        )
        noteId2 <- ZIO.serviceWithZIO[NotesRepositoryAlg](
          _.insertNotesTable("Note")
        )
        tagId1 <- ZIO.serviceWithZIO[TagsRepositoryAlg](_.insertTagsTable("Science"))
        tagId2 <- ZIO.serviceWithZIO[TagsRepositoryAlg](_.insertTagsTable("Math"))
        tagId3 <- ZIO.serviceWithZIO[TagsRepositoryAlg](_.insertTagsTable("Manga"))
        repo <- ZIO.service[NotesTagsRepositoryAlg]
        _ <- repo.insertIntoNotesTagsTable(tagId1, noteId1)
        _ <- repo.insertIntoNotesTagsTable(tagId1, noteId2)
        _ <- repo.insertIntoNotesTagsTable(tagId2, noteId1)
        _ <- repo.insertIntoNotesTagsTable(tagId3, noteId1)
        result <- repo.getAllTagsByNoteID(noteId1)
      } yield assertTrue(result.length == 3))
        .provide(
          NotesRepository.live,
          TagsRepository.live,
          NotesTagsRepository.live,
          PostgresZioJdbcContextLayer.live,
          DataSource.fromPrefix("ctx")
        )
    },
    test("should get all rows in the note_tags table") {
      (for {
        noteId <- ZIO.serviceWithZIO[NotesRepositoryAlg](
          _.insertNotesTable("Note")
        )
        tagId1 <- ZIO.serviceWithZIO[TagsRepositoryAlg](
          _.insertTagsTable("Tag")
        )
        tagId2 <- ZIO.serviceWithZIO[TagsRepositoryAlg](
          _.insertTagsTable("Science")
        )
        _ <- ZIO.serviceWithZIO[NotesTagsRepositoryAlg](
          _.insertIntoNotesTagsTable(tagId1, noteId)
        )
        _ <- ZIO.serviceWithZIO[NotesTagsRepositoryAlg](
          _.insertIntoNotesTagsTable(tagId2, noteId)
        )
        result <- ZIO.serviceWithZIO[NotesTagsRepositoryAlg](
          _.getAllTagIDsByNoteID(noteId)
        )
      } yield assertTrue(result == List(tagId1, tagId2)))
        .provide(
          NotesRepository.live,
          TagsRepository.live,
          NotesTagsRepository.live,
          PostgresZioJdbcContextLayer.live,
          DataSource.fromPrefix("ctx")
        )
    },
    test(
      "should error when attempting to insert the same noteID, tagID combination"
    ) {
      (for {
        noteId <- ZIO.serviceWithZIO[NotesRepositoryAlg](
          _.insertNotesTable("Note")
        )
        tagId <- ZIO.serviceWithZIO[TagsRepositoryAlg](_.insertTagsTable("Tag"))
        _ <- ZIO.serviceWithZIO[NotesTagsRepositoryAlg](
          _.insertIntoNotesTagsTable(tagId, noteId)
        )
        result <- ZIO
          .serviceWithZIO[NotesTagsRepositoryAlg](
            _.insertIntoNotesTagsTable(tagId, noteId)
          )
          .flip
      } yield assertTrue(
        result.isInstanceOf[org.postgresql.util.PSQLException]
      ))
        .provide(
          NotesRepository.live,
          TagsRepository.live,
          NotesTagsRepository.live,
          PostgresZioJdbcContextLayer.live,
          DataSource.fromPrefix("ctx")
        )
    }
  ) @@ before(
    ZIO
      .serviceWithZIO[FlywayMigrationsAlg](_.migrate)
      .provide(
        FlywayMigrations.live,
        ApplicationConfiguration.live(ConfigSource.default)
      )
  ) @@ after(
    ZIO
      .serviceWithZIO[FlywayMigrationsAlg](_.clean)
      .provide(
        FlywayMigrations.live,
        ApplicationConfiguration.live(ConfigSource.default)
      )
      .ignore
  ) @@ sequential

}
