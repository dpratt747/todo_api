package db.repository

import io.getquill._
import zio._

import java.sql.SQLException
import java.time.OffsetDateTime
import javax.sql.DataSource

trait TagsRepositoryAlg {
  def getByTagID(
      tagToRetrieve: Long
  ): ZIO[DataSource, SQLException, Option[TagsRepository.TagsTable]]
  def insertTagsTable(tag: String): ZIO[DataSource, SQLException, Long]
  def getAllTags: ZIO[DataSource, SQLException, List[TagsRepository.TagsTable]]
}

final case class TagsRepository(
    private val ctx: PostgresZioJdbcContext[SnakeCase.type]
) extends TagsRepositoryAlg {
  import TagsRepository._
  import ctx._

  def getByTagID(
      tagToRetrieve: Long
  ): ZIO[DataSource, SQLException, Option[TagsTable]] = {
    val q = quote {
      query[TagsTable].filter(_.id == lift(tagToRetrieve)).take(1)
    }
    ctx.run(q).map(_.headOption)
  }

  override def insertTagsTable(
      tag: String
  ): ZIO[DataSource, SQLException, Long] = {
    val q = quote {
      query[TagsTable].insert(_.tag -> lift(tag.toUpperCase)).returning(_.id)
    }
    ctx.run(q)
  }

  override def getAllTags: ZIO[DataSource, SQLException, List[TagsTable]] = {
    val q = quote {
      query[TagsTable]
    }
    ctx.run(q)
  }
}

object TagsRepository {
  final case class TagsTable(
      id: Long,
      tag: String,
      createdAt: OffsetDateTime
  )

  val live: ZLayer[PostgresZioJdbcContext[
    SnakeCase.type
  ], Nothing, TagsRepository] = ZLayer.fromFunction(TagsRepository.apply _)

}
