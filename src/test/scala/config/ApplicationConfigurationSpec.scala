package config

import config.ApplicationConfiguration._
import pureconfig.ConfigSource
import pureconfig.error.ConfigReaderFailures
import zio.test.Assertion.failsWithA
import zio.test._
import zio.{Scope, ZIO}

object ApplicationConfigurationSpec extends ZIOSpecDefault {
  def spec =
    suite("ApplicationConfiguration")(
      test("should load the configuration from the application.conf file") {
        (for {
          service <- ZIO.service[Configuration]
        } yield assertTrue(service == Configuration(Server(8080), Context(
          "org.postgresql.ds.PGSimpleDataSource",
          DataSourceConfig(5432, "postgres", "postgres", "todo", "localhost"),
          30000
        )))).provide(ApplicationConfiguration.live(ConfigSource.default))
      },
      test("should load the configuration from a valid hocon string") {

        val source = ConfigSource.string(
          """
            |server {
            |
            |    port = 8080
            |
            |}
            |
            |ctx {
            |
            |    dataSourceClassName=org.postgresql.ds.PGSimpleDataSource
            |
            |    dataSource {
            |        portNumber = 5432
            |        user = postgres
            |        password = postgres
            |        databaseName = todo
            |        serverName = localhost
            |    }
            |
            |    connectionTimeout = 30000
            |    connectionTimeout = ${?POSTGRES_CONNECTION_TIMEOUT}
            |}
            |""".stripMargin)

        (for {
          service <- ZIO.service[Configuration]
        } yield assertTrue(service == Configuration(Server(8080), Context(
          "org.postgresql.ds.PGSimpleDataSource",
          DataSourceConfig(5432, "postgres", "postgres", "todo", "localhost"),
          30000
        )))).provide(ApplicationConfiguration.live(source))
      },
      test("should fail to load the configuration from an invalid hocon string") {
        val source = ConfigSource.string(
          """
            |database {
            |  host = "localhost"
            |  port = "this is invalid"
            |  user = "postgres"
            |  password = "postgres"
            |}
            |""".stripMargin)

        (for {
          failures <- ApplicationConfiguration.live(source).build.exit
        } yield assert(failures)(failsWithA[ConfigReaderFailures])).provide(Scope.default)
      }
    )
}
