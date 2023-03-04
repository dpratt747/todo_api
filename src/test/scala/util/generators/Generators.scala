package util.generators

import domain.CustomTypes.NoteId
import domain.Note
import zio.test.Gen

trait Generators {
  val noteGen: Gen[Any, Note] =
    Gen.alphaNumericString.map(domain.Note(_, List.empty))
  val noteIdGen: Gen[Any, NoteId.Type] = Gen.long.map(NoteId(_))
}
