package db.persistence

import db.context.PostgresZioJdbcContextLayer
import io.getquill.jdbczio.Quill.DataSource
import util.mocks._
import zio._
import zio.mock._
import zio.test._

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
          result <- ZIO.serviceWithZIO[NotesTagsPersistence](
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
    test("should insert a note and also the tags provided") {
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
                .InsertTagsTable(
                  Assertion.equalTo(tag),
                  Expectation.valueZIO(_ => ZIO.succeed(1L))
                )
                .exactly(1)
            )
            .reduce(_ ++ _)
            .toLayer

          val mockNotesTagsRepoLayer = tags
            .map(_ =>
              NotesTagsRepositoryMock.InsertIntoNotesTagsTable(
                  Assertion.anything,
                  Expectation.valueZIO(_ => ZIO.succeed(1L))
                )
                .exactly(1)
            )
            .reduce(_ ++ _)
            .toLayer

          (for {
            result <- ZIO.serviceWithZIO[NotesTagsPersistence](
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
    }
  )

}
