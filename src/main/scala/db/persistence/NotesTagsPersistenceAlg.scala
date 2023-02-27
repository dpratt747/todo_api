package db.persistence

import db.repository._
import domain._
import io.getquill._
import zio._

import javax.sql.DataSource

trait NotesTagsPersistenceAlg {
  def createNote(
      note: String,
      tags: List[String]
  ): ZIO[DataSource, Throwable, Long]

  def getNote(
      noteID: Long
  ): ZIO[DataSource, Throwable, Option[Note]]

}

final case class NotesTagsPersistence(
    private val ctx: PostgresZioJdbcContext[SnakeCase.type],
    private val notesRepo: NotesRepositoryAlg,
    private val tagsRepo: TagsRepositoryAlg,
    private val notesTagsRepository: NotesTagsRepositoryAlg
) extends NotesTagsPersistenceAlg {

  override def getNote(
      noteID: Long
  ): ZIO[DataSource, Throwable, Option[Note]] =
    notesRepo.getNoteByNoteID(noteID).flatMap {
      case Some(note) =>
        notesTagsRepository.getAllTagsByNoteID(note.id).map { tags =>
          Some(Note(note.note, tags.map(_.tag)))
        }
      case None => ZIO.succeed(None)
    }

  override def createNote(
      note: String,
      tags: List[String]
  ): ZIO[DataSource, Throwable, Long] = {
    ctx.transaction(for {
      noteID <- notesRepo.insertNotesTable(note)
      tagIDs <- ZIO.foreach(tags)(tag => tagsRepo.insertTagsTable(tag))
      _ <- ZIO.foreachDiscard(tagIDs)(tagID =>
        notesTagsRepository.insertIntoNotesTagsTable(tagID, noteID)
      )
    } yield noteID)
  }

}

object NotesTagsPersistence {
  val live
      : ZLayer[PostgresZioJdbcContext[SnakeCase.type] with NotesRepositoryAlg with TagsRepositoryAlg with NotesTagsRepositoryAlg, Nothing, NotesTagsPersistence] =
    ZLayer.fromFunction(NotesTagsPersistence.apply _)
}
