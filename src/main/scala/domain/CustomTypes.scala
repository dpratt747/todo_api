package domain

import io.getquill.mirrorContextWithQueryProbing._
import zio.prelude._

object CustomTypes {
  object NoteId extends Newtype[Long] {
    // This is needed for the quill context to work
    implicit val encodeNoteId: MappedEncoding[Long, NoteId] =
      MappedEncoding[Long, NoteId](NoteId(_))
    implicit val decodeNoteId: MappedEncoding[NoteId, Long] =
      MappedEncoding[NoteId, Long](NoteId.unwrap)
  }
  type NoteId = NoteId.Type
}
