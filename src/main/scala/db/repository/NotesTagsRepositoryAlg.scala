package db.repository

import db.repository.TagsRepository.TagsTable
import domain.CustomTypes.NoteId
import io.getquill._
import zio._

import java.sql.SQLException
import javax.sql.DataSource

trait NotesTagsRepositoryAlg {
  def insertIntoNotesTagsTable(
      tagID: Long,
      noteID: NoteId
  ): ZIO[DataSource, SQLException, Long]

  def getAllTagIDsByNoteID(
      noteID: NoteId
  ): ZIO[DataSource, SQLException, List[Long]]

  def getAllTagsByNoteID(
      noteID: NoteId
  ): ZIO[DataSource, SQLException, List[TagsTable]]
}

final case class NotesTagsRepository(
    private val ctx: PostgresZioJdbcContext[SnakeCase.type]
) extends NotesTagsRepositoryAlg {
  import NotesTagsRepository._
  import ctx._

  override def insertIntoNotesTagsTable(
      tagID: Long,
      noteID: NoteId
  ): ZIO[DataSource, SQLException, Long] = {
    val q = quote {
      query[NotesTagsTable]
        .insert(
          _.tagId -> lift(tagID),
          _.noteId -> lift(noteID)
        )
        .returning(_.id)
    }
    ctx.run(q)
  }

  override def getAllTagsByNoteID(
      noteID: NoteId
  ): ZIO[DataSource, SQLException, List[TagsTable]] = {
    val q = quote {
      query[NotesTagsTable]
        .join(query[TagsTable])
        .on(_.tagId == _.id)
        .filter(_._1.noteId == lift(noteID))
        .map(_._2)
    }
    ctx.run(q)
  }

  override def getAllTagIDsByNoteID(
      noteID: NoteId
  ): ZIO[DataSource, SQLException, List[Long]] = {
    val q = quote {
      query[NotesTagsTable].filter(_.noteId == lift(noteID)).map(_.tagId)
    }
    ctx.run(q)
  }
}

object NotesTagsRepository {
  private final case class NotesTagsTable(
      id: Long,
      noteId: NoteId,
      tagId: Long
  )

  val live: ZLayer[PostgresZioJdbcContext[
    SnakeCase.type
  ], Nothing, NotesTagsRepositoryAlg] =
    ZLayer.fromFunction(NotesTagsRepository.apply _)

}
