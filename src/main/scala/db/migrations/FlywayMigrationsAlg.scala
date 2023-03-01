package db.migrations

import config.ApplicationConfiguration
import config.ApplicationConfiguration.Configuration
import org.flywaydb.core.{Flyway, _}
import zio._

trait FlywayMigrationsAlg {
  def migrate: Task[Unit]

  def clean: Task[Unit]
}

final case class FlywayMigrations(private val config: ApplicationConfiguration.Context)
  extends FlywayMigrationsAlg {

  private val flywayIO: Task[Flyway] = ZIO.attempt(
    Flyway
      .configure()
      .cleanDisabled(false)
      .dataSource(
        config.connectionString,
        config.dataSource.user,
        config.dataSource.password
      )
      .load()
  )

  override def migrate: Task[Unit] =
    for {
      _ <- ZIO.logInfo("Running migrations")
      flyway <- flywayIO
      _ <- ZIO.attempt(flyway.migrate())
    } yield ()

  override def clean: Task[Unit] =
    for {
      _ <- ZIO.logInfo("Cleaning migrations")
      flyway <- flywayIO
      _ <- ZIO.attempt(flyway.clean())
    } yield ()

}

object FlywayMigrations {

  val live: ZLayer[Configuration, Nothing, FlywayMigrationsAlg] =
    ZLayer.fromZIO(
      for {
        config <- ZIO.serviceWith[Configuration](_.ctx)
      } yield FlywayMigrations(config)
    )

}
