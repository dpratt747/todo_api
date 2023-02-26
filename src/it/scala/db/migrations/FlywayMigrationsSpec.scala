package db.migrations

import config.ApplicationConfiguration
import config.ApplicationConfiguration.Configuration
import org.flywaydb.core._
import pureconfig.ConfigSource
import zio.ZIO
import zio.test._

object FlywayMigrationsSpec extends ZIOSpecDefault {
  override def spec =
    suite("FlywayMigrations")(
      test("can connect and run the migrations and clean") {
        (for {
          flyway <- ZIO.service[FlywayMigrationsAlg]
          config <- ZIO.serviceWith[Configuration](_.ctx)
          flywayForInfo <- ZIO.attempt(
            Flyway
              .configure()
              .cleanDisabled(false)
              .dataSource(config.connectionString, config.dataSource.user, config.dataSource.password)
              .load()
          )
          _ <- flyway.migrate
          appliedMigrations <- ZIO.attempt(flywayForInfo.info().applied().length)
          _ <- flyway.clean
          appliedMigrationsAfterClean <- ZIO.attempt(flywayForInfo.info().applied().length)
        } yield assertTrue(appliedMigrations == 1 && appliedMigrationsAfterClean == 0))
          .provide(FlywayMigrations.live, ApplicationConfiguration.live(ConfigSource.default))
      }
    )
}