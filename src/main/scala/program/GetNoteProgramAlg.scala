package program

import db.persistence._
import domain.Note
import zio._
trait GetNoteProgramAlg {
  def getNote(noteID: Long): Task[Option[Note]]
}

final case class GetNoteProgram(
    private val persistence: NotesTagsPersistenceAlg
) extends GetNoteProgramAlg {

  override def getNote(noteID: Long): Task[Option[Note]] =
    persistence.getNote(noteID)

}

object GetNoteProgram {
  val live: ZLayer[NotesTagsPersistenceAlg, Nothing, GetNoteProgramAlg] =
    ZLayer.fromFunction(GetNoteProgram.apply _)
}
