package db.repository

import config.ApplicationConfiguration
import db.context.PostgresZioJdbcContextLayer
import db.migrations.{FlywayMigrations, FlywayMigrationsAlg}
import io.getquill.jdbczio.Quill.DataSource
import pureconfig.ConfigSource
import zio._
import zio.test.TestAspect._
import zio.test._

object TagsRepositorySpec extends ZIOSpecDefault {
  def spec = suite("TagsRepository")(
    test("should insert a tag into the tags table") {
      (for {
        repo <- ZIO.service[TagsRepositoryAlg]
        result <- repo.insertTagsTable("Science")
      } yield assertTrue(result == 1))
        .provide(
          TagsRepository.live,
          PostgresZioJdbcContextLayer.live,
          DataSource.fromPrefix("ctx")
        )
    },
    test("should get all the tags that are stored") {
      (for {
        repo <- ZIO.service[TagsRepositoryAlg]
        _ <- repo.insertTagsTable("Tag 1")
        _ <- repo.insertTagsTable("Tag 2")
        _ <- repo.insertTagsTable("Tag 3")
        result <- repo.getAllTags
        insertedTags = result.map(_.tag)
      } yield assertTrue(insertedTags == List("TAG 1", "TAG 2", "TAG 3")))
        .provide(
          TagsRepository.live,
          PostgresZioJdbcContextLayer.live,
          DataSource.fromPrefix("ctx")
        )
    },
    test("should get the stored tag by its tagID") {
      (for {
        repo <- ZIO.service[TagsRepositoryAlg]
        _ <- repo.insertTagsTable("Tag 1")
        tagToRetrieve <- repo.insertTagsTable("Tag 2")
        _ <- repo.insertTagsTable("Tag 3")
        result <- repo.getByTagID(tagToRetrieve)
        insertedTags = result.map(_.tag)
      } yield assertTrue(insertedTags.contains("TAG 2")))
        .provide(
          TagsRepository.live,
          PostgresZioJdbcContextLayer.live,
          DataSource.fromPrefix("ctx")
        )
    },
    test("should get the stored tag by its name") {
      (for {
        repo <- ZIO.service[TagsRepositoryAlg]
        _ <- repo.insertTagsTable("Tag 1")
        tagToRetrieve <- repo.insertTagsTable("Tag 2")
        _ <- repo.insertTagsTable("Tag 3")
        result <- repo.getTagIDByTag("tag 2")
      } yield assertTrue(result.contains(tagToRetrieve)))
        .provide(
          TagsRepository.live,
          PostgresZioJdbcContextLayer.live,
          DataSource.fromPrefix("ctx")
        )
    },
    test("should error when duplicate tags are inserted") {
      (for {
        repo <- ZIO.service[TagsRepositoryAlg]
        _ <- repo.insertTagsTable("Tag 1")
        q2 <- repo.insertTagsTable("Tag 1").flip
      } yield assertTrue(q2.isInstanceOf[org.postgresql.util.PSQLException]))
        .provide(
          TagsRepository.live,
          PostgresZioJdbcContextLayer.live,
          DataSource.fromPrefix("ctx")
        )
    }
  ) @@ before(
    ZIO
      .serviceWithZIO[FlywayMigrationsAlg](_.migrate)
      .provide(
        FlywayMigrations.live,
        ApplicationConfiguration.live(ConfigSource.default)
      )
  ) @@ after(
    ZIO
      .serviceWithZIO[FlywayMigrationsAlg](_.clean)
      .provide(
        FlywayMigrations.live,
        ApplicationConfiguration.live(ConfigSource.default)
      )
      .ignore
  ) @@ sequential

}
