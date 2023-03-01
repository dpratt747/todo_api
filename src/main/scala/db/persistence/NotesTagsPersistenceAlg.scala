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
  ): Task[Long]

  def getNote(
      noteID: Long
  ): Task[Option[Note]]

}

final case class NotesTagsPersistence(
    private val ctx: PostgresZioJdbcContext[SnakeCase.type],
    private val notesRepo: NotesRepositoryAlg,
    private val tagsRepo: TagsRepositoryAlg,
    private val notesTagsRepository: NotesTagsRepositoryAlg,
    private val dataSource: DataSource
) extends NotesTagsPersistenceAlg {

  override def getNote(
      noteID: Long
  ): Task[Option[Note]] =
    notesRepo
      .getNoteByNoteID(noteID)
      .flatMap {
        case Some(note) =>
          notesTagsRepository.getAllTagsByNoteID(note.id).map { tags =>
            Some(Note(note.note, tags.map(_.tag)))
          }
        case None => ZIO.succeed(None)
      }
      .provideSomeLayer(ZLayer.succeed(dataSource))

  override def createNote(
      note: String,
      tags: List[String]
  ): Task[Long] =
    ctx
      .transaction(for {
        noteID <- notesRepo.insertNotesTable(note)
        tagIDs <- ZIO.foreach(tags)(tag =>
          tagsRepo
            .getTagIDByTag(tag)
            .flatMap {
              case Some(tagID) => ZIO.succeed(tagID)
              case None        => tagsRepo.insertTagsTable(tag)
            }
        )
        _ <- ZIO.foreachDiscard(tagIDs)(tagID =>
          notesTagsRepository.insertIntoNotesTagsTable(tagID, noteID)
        )
      } yield noteID)
      .provideSomeLayer(ZLayer.succeed(dataSource))

}

object NotesTagsPersistence {
  val live
      : ZLayer[PostgresZioJdbcContext[SnakeCase.type] with NotesRepositoryAlg with TagsRepositoryAlg with NotesTagsRepositoryAlg with DataSource, Nothing, NotesTagsPersistenceAlg] =
    ZLayer.fromFunction(NotesTagsPersistence.apply _)
}
