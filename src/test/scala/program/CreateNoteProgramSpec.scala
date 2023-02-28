package program

import util.mocks._
import zio._
import zio.mock.Expectation
import zio.test._

object CreateNoteProgramSpec extends ZIOSpecDefault {
  def spec = suite("CreateNoteProgram")(
    test("should create a note") {
      val noteGen = Gen.alphaNumericString.map(domain.Note(_, List.empty))
      checkAll(noteGen) { note =>
        val notesTagsPersistenceMockLayer = NotesTagsPersistenceMock
          .CreateNote(
            Assertion.equalTo((note.text, note.tags)),
            Expectation.valueZIO(_ => ZIO.succeed(1L))
          )
          .exactly(1)
          .toLayer

        val program = for {
          res <- ZIO.serviceWithZIO[CreateNoteProgramAlg](_.createNote(note))
        } yield assertTrue(res == 1L)
        program.provide(
          CreateNoteProgram.live,
          notesTagsPersistenceMockLayer
        )
      }
    }
  )
}
