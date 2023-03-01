package db.persistence

import db.context.PostgresZioJdbcContextLayer
import db.repository.NotesRepository.NotesTable
import db.repository.TagsRepository.TagsTable
import domain._
import io.getquill.jdbczio.Quill.DataSource
import util.mocks._
import zio._
import zio.mock._
import zio.test._

import java.time.OffsetDateTime

object NotesTagsPersistenceSpec extends ZIOSpecDefault {

  def spec = suite("NotesTagsPersistence")(
    test("should insert a note when there are no tags provided") {
      checkAll(Gen.alphaNumericString) { note =>
        val mockNotesRepoLayer = NotesRepositoryMock
          .InsertNotesTable(
            Assertion.equalTo(note),
            Expectation.valueZIO(_ => ZIO.succeed(1L))
          )
          .exactly(1)
          .toLayer

        (for {
          result <- ZIO.serviceWithZIO[NotesTagsPersistenceAlg](
            _.createNote(note, List.empty)
          )
        } yield assertTrue(result == 1L))
          .provide(
            NotesTagsPersistence.live,
            PostgresZioJdbcContextLayer.live,
            mockNotesRepoLayer,
            DataSource.fromPrefix("ctx"),
            TagsRepositoryMock.empty,
            NotesTagsRepositoryMock.empty
          )
      }
    },
    test("should insert a note and get the tags if that tag already exists") {
      checkAll(Gen.alphaNumericString, Gen.listOf(Gen.alphaNumericString)) {
        (note, tags) =>
          val mockNotesRepoLayer = NotesRepositoryMock
            .InsertNotesTable(
              Assertion.equalTo(note),
              Expectation.valueZIO(_ => ZIO.succeed(1L))
            )
            .exactly(1)
            .toLayer

          val mockTagsRepoLayer = tags
            .map(tag =>
              TagsRepositoryMock
                .GetTagIDByTag(
                  Assertion.equalTo(tag),
                  Expectation.valueZIO(_ => ZIO.succeed(Some(1L)))
                )
                .exactly(1)
            )
            .reduce(_ ++ _)
            .toLayer

          val mockNotesTagsRepoLayer = tags
            .map(_ =>
              NotesTagsRepositoryMock
                .InsertIntoNotesTagsTable(
                  Assertion.anything,
                  Expectation.valueZIO(_ => ZIO.succeed(1L))
                )
                .exactly(1)
            )
            .reduce(_ ++ _)
            .toLayer

          (for {
            result <- ZIO.serviceWithZIO[NotesTagsPersistenceAlg](
              _.createNote(note, tags)
            )
          } yield assertTrue(result == 1L))
            .provide(
              NotesTagsPersistence.live,
              PostgresZioJdbcContextLayer.live,
              mockNotesRepoLayer,
              DataSource.fromPrefix("ctx"),
              mockTagsRepoLayer,
              mockNotesTagsRepoLayer
            )
      }
    },
    test("should insert a note and insert a tag if it does not exist") {
      checkAll(Gen.alphaNumericString, Gen.listOf(Gen.alphaNumericString)) {
        (note, tags) =>
          val mockNotesRepoLayer = NotesRepositoryMock
            .InsertNotesTable(
              Assertion.equalTo(note),
              Expectation.valueZIO(_ => ZIO.succeed(1L))
            )
            .exactly(1)
            .toLayer

          val mockNotesTagsRepoLayer = tags
            .map(_ =>
              NotesTagsRepositoryMock
                .InsertIntoNotesTagsTable(
                  Assertion.anything,
                  Expectation.valueZIO(_ => ZIO.succeed(1L))
                )
                .exactly(1)
            )
            .reduce(_ ++ _)
            .toLayer

          val mockTagsRepoLayer = tags
            .map(tag =>
              TagsRepositoryMock
                .GetTagIDByTag(
                  Assertion.equalTo(tag),
                  Expectation.valueZIO(_ => ZIO.succeed(None))
                )
                .exactly(1) and
                TagsRepositoryMock
                  .InsertTagsTable(
                    Assertion.equalTo(tag),
                    Expectation.valueZIO(_ => ZIO.succeed(1L))
                  )
                  .exactly(1)
            )
            .reduce(_ ++ _)
            .toLayer

          (for {
            result <- ZIO.serviceWithZIO[NotesTagsPersistenceAlg](
              _.createNote(note, tags)
            )
          } yield assertTrue(result == 1L))
            .provide(
              NotesTagsPersistence.live,
              PostgresZioJdbcContextLayer.live,
              mockNotesRepoLayer,
              DataSource.fromPrefix("ctx"),
              mockTagsRepoLayer,
              mockNotesTagsRepoLayer
            )
      }
    },
    test("should get a note that has been inserted") {

      val noteGen = for {
        string <- Gen.alphaNumericString
        tags <- Gen.listOf(Gen.alphaNumericString)
      } yield Note(string, tags)

      checkAll(noteGen) { note =>
        val mockNotesRepoLayer = NotesRepositoryMock
          .GetNoteByNoteID(
            Assertion.equalTo(1L),
            Expectation.valueZIO(id =>
              ZIO.succeed(
                Option(NotesTable(id, note.text, OffsetDateTime.now()))
              )
            )
          )
          .toLayer

        val notesTagsRepoLayer = NotesTagsRepositoryMock
          .GetAllTagsByNoteID(
            Assertion.equalTo(1L),
            Expectation.value(
              note.tags.map(TagsTable(1L, _, OffsetDateTime.now()))
            )
          )
          .toLayer

        (for {
          result <- ZIO.serviceWithZIO[NotesTagsPersistenceAlg](
            _.getNote(1L)
          )
        } yield assertTrue(result.contains(note)))
          .provide(
            NotesTagsPersistence.live,
            PostgresZioJdbcContextLayer.live,
            mockNotesRepoLayer,
            DataSource.fromPrefix("ctx"),
            TagsRepositoryMock.empty,
            notesTagsRepoLayer
          )
      }
    },
    test("should not try to get tags if there is no note") {

      val mockNotesRepoLayer = NotesRepositoryMock
        .GetNoteByNoteID(
          Assertion.equalTo(1L),
          Expectation.valueZIO(_ => ZIO.succeed(Option.empty[NotesTable]))
        )
        .toLayer

      (for {
        result <- ZIO.serviceWithZIO[NotesTagsPersistenceAlg](
          _.getNote(1L)
        )
      } yield assertTrue(result.isEmpty))
        .provide(
          NotesTagsPersistence.live,
          PostgresZioJdbcContextLayer.live,
          mockNotesRepoLayer,
          DataSource.fromPrefix("ctx"),
          TagsRepositoryMock.empty,
          NotesTagsRepositoryMock.empty
        )
    }
  )

}
