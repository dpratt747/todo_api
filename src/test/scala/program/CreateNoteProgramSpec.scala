package program

import domain.CustomTypes._
import util.generators.Generators
import util.mocks._
import zio._
import zio.mock.Expectation
import zio.test._

object CreateNoteProgramSpec extends ZIOSpecDefault with Generators {
  def spec = suite("CreateNoteProgram")(
    test("should create a note") {
      checkAll(noteGen, noteIdGen) { (note, noteId) =>
        val notesTagsPersistenceMockLayer = NotesTagsPersistenceMock
          .CreateNote(
            Assertion.equalTo((note.text, note.tags)),
            Expectation.valueZIO(_ => ZIO.succeed(noteId))
          )
          .exactly(1)
          .toLayer

        val program = for {
          res <- ZIO.serviceWithZIO[CreateNoteProgramAlg](_.createNote(note))
        } yield assertTrue(res == noteId)
        program.provide(
          CreateNoteProgram.live,
          notesTagsPersistenceMockLayer
        )
      }
    }
  )
}
