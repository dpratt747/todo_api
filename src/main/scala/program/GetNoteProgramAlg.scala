package program

import db.persistence._
import domain.CustomTypes.NoteId
import domain.Note
import zio._
trait GetNoteProgramAlg {
  def getNote(noteID: NoteId): Task[Option[Note]]
}

final case class GetNoteProgram(
    private val persistence: NotesTagsPersistenceAlg
) extends GetNoteProgramAlg {

  override def getNote(noteID: NoteId): Task[Option[Note]] =
    persistence.getNote(noteID)

}

object GetNoteProgram {
  val live: ZLayer[NotesTagsPersistenceAlg, Nothing, GetNoteProgramAlg] =
    ZLayer.fromFunction(GetNoteProgram.apply _)
}
