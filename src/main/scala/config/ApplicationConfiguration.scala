package config

import pureconfig._
import pureconfig.error.ConfigReaderFailures
import pureconfig.generic.ProductHint
import pureconfig.generic.auto._
import zio.{ZIO, ZLayer}

object ApplicationConfiguration {

  implicit def hint[A]: ProductHint[A] =
    ProductHint[A](ConfigFieldMapping(CamelCase, CamelCase))

  final case class DataSourceConfig(
      portNumber: Int,
      user: String,
      password: String,
      databaseName: String,
      serverName: String
  )

  final case class Context(
      dataSourceClassName: String,
      dataSource: DataSourceConfig,
      connectionTimeout: Long
  ) {
    import dataSource._
    val connectionString: String =
      s"jdbc:postgresql://$serverName:$portNumber/$databaseName"
  }

  final case class Configuration(
      ctx: Context
  )

  def live(
      configSource: ConfigSource
  ): ZLayer[Any, ConfigReaderFailures, Configuration] =
    ZLayer.fromZIO(
      ZIO.fromEither(
        configSource.load[ApplicationConfiguration.Configuration]
      )
    )
}
