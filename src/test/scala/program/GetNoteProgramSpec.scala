package program

import util.mocks._
import zio._
import zio.mock.Expectation
import zio.test._

object GetNoteProgramSpec extends ZIOSpecDefault {
  def spec = suite("GetNoteProgram")(
    test("should get a note") {
      val noteGen = Gen.alphaNumericString.map(domain.Note(_, List.empty))
      checkAll(noteGen, Gen.long) { (note, noteId) =>
        val notesTagsPersistenceMockLayer = NotesTagsPersistenceMock
          .GetNote(
            Assertion.equalTo(noteId),
            Expectation.valueZIO(_ => ZIO.succeed(Some(note)))
          )
          .exactly(1)
          .toLayer

        val program = for {
          res <- ZIO.serviceWithZIO[GetNoteProgramAlg](_.getNote(noteId))
        } yield assertTrue(res.contains(note))

        program.provide(
          GetNoteProgram.live,
          notesTagsPersistenceMockLayer
        )
      }
    }
  )
}
