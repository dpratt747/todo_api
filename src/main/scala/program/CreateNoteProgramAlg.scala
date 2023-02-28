package program

import db.persistence._
import zio._
trait CreateNoteProgramAlg {
  def createNote(note: domain.Note): Task[Long]
}

final case class CreateNoteProgram(
    private val persistence: NotesTagsPersistenceAlg
) extends CreateNoteProgramAlg {
  override def createNote(
      note: domain.Note
  ): Task[Long] =
    persistence.createNote(note.text, note.tags)
}

object CreateNoteProgram {
  val live: ZLayer[NotesTagsPersistenceAlg, Nothing, CreateNoteProgram] =
    ZLayer.fromFunction(CreateNoteProgram.apply _)
}
