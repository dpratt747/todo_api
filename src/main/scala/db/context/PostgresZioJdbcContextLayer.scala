package db.context

import io.getquill._
import zio.{ULayer, ZLayer}

object PostgresZioJdbcContextLayer {
  def live: ULayer[PostgresZioJdbcContext[SnakeCase.type]] = ZLayer.succeed(new PostgresZioJdbcContext(SnakeCase))
}
