package program

import db.persistence._
import domain.CustomTypes._
import zio._
trait CreateNoteProgramAlg {
  def createNote(note: domain.Note): Task[NoteId]
}

final case class CreateNoteProgram(
    private val persistence: NotesTagsPersistenceAlg
) extends CreateNoteProgramAlg {
  override def createNote(
      note: domain.Note
  ): Task[NoteId] =
    persistence.createNote(note.text, note.tags)
}

object CreateNoteProgram {
  val live: ZLayer[NotesTagsPersistenceAlg, Nothing, CreateNoteProgramAlg] =
    ZLayer.fromFunction(CreateNoteProgram.apply _)
}
