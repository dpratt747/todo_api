package db.repository

import io.getquill._
import zio._

import java.sql.SQLException
import java.time.OffsetDateTime
import javax.sql.DataSource

trait NotesRepositoryAlg {
  def getNoteByNoteID(noteID: Long): ZIO[DataSource, SQLException, Option[NotesRepository.NotesTable]]

  def insertNotesTable(note: String): ZIO[DataSource, SQLException, Long]
  def getAllNotes
      : ZIO[DataSource, SQLException, List[NotesRepository.NotesTable]]
}

final case class NotesRepository(
    private val ctx: PostgresZioJdbcContext[SnakeCase.type]
) extends NotesRepositoryAlg {
  import NotesRepository._
  import ctx._

  override def insertNotesTable(
      note: String
  ): ZIO[DataSource, SQLException, Long] = {
    val q = quote {
      query[NotesTable].insert(_.note -> lift(note)).returning(_.id)
    }

    ctx.run(q)
  }

  override def getNoteByNoteID(noteID: Long): ZIO[DataSource, SQLException, Option[NotesTable]] = {
    val q = quote {
      query[NotesTable].filter(_.id == lift(noteID)).take(1)
    }
    ctx.run(q).map(_.headOption)
  }

  override def getAllNotes: ZIO[DataSource, SQLException, List[NotesTable]] = {
    val q = quote {
      query[NotesTable]
    }
    ctx.run(q)
  }
}

object NotesRepository {
  final case class NotesTable(
      id: Long,
      note: String,
      createdAt: OffsetDateTime
  )

  val live: ZLayer[PostgresZioJdbcContext[
    SnakeCase.type
  ], Nothing, NotesRepositoryAlg] = ZLayer.fromFunction(NotesRepository.apply _)

}
