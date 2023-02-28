package db.persistence

import db.repository._
import domain._
import org.postgresql.util.PSQLException
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
    (for {
      noteID <- notesRepo.insertNotesTable(note)
      tagIDs <- ZIO.foreach(tags)(tag =>
        tagsRepo
          .insertTagsTable(tag)
          .catchSome {
            case e: PSQLException if e.getSQLState contains "23505" =>
              for {
                idO <- tagsRepo.getTagIDByTag(tag)
                id <- ZIO
                  .fromOption(idO)
                  .orElseFail(
                    new RuntimeException(
                      s"Tag $tag was not found in the database, but was not inserted either"
                    )
                  )
              } yield id
          }
      )
      _ <- ZIO.foreachDiscard(tagIDs)(tagID =>
        notesTagsRepository.insertIntoNotesTagsTable(tagID, noteID)
      )
    } yield noteID)
      .provideSomeLayer(ZLayer.succeed(dataSource))

}

object NotesTagsPersistence {
  val live: ZLayer[
    NotesRepositoryAlg
      with TagsRepositoryAlg
      with NotesTagsRepositoryAlg
      with DataSource,
    Nothing,
    NotesTagsPersistence
  ] =
    ZLayer.fromFunction(NotesTagsPersistence.apply _)
}
