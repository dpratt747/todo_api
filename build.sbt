ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.10"

Global / onChangedBuildSource := ReloadOnSourceChanges

lazy val root = (project in file("."))
  .configs(IntegrationTest)
  .settings(
    name := "TodoApp",
    Defaults.itSettings
  )

scalacOptions ++= Seq(
  "-Ymacro-annotations"
)

addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")

lazy val zioVersion = "2.0.9"
lazy val zioMockVersion = "1.0.0-RC9"
lazy val zioInteropCatsVersion = "23.0.0.1"
lazy val quillVersion = "4.6.0"
lazy val pureConfigVersion = "0.17.2"
lazy val flywayVersion = "9.15.0"
lazy val postgresVersion = "42.5.4"
lazy val zioHttpVersion = "0.0.4"

lazy val testDependencies = Seq(
  "dev.zio" %% "zio-test" % zioVersion % "it,test",
  "dev.zio" %% "zio-test-sbt" % zioVersion % "it,test",
  "dev.zio" %% "zio-test-magnolia" % zioVersion % "it,test",
  "dev.zio" %% "zio-mock" % zioMockVersion % "it,test"
)

libraryDependencies ++= Seq(
  "org.flywaydb" % "flyway-core" % flywayVersion,
  "org.postgresql" % "postgresql" % postgresVersion,
  "com.github.pureconfig" %% "pureconfig" % pureConfigVersion,
  "io.getquill" %% "quill-jdbc-zio" % quillVersion,
  "dev.zio" %% "zio" % zioVersion,
  "dev.zio" %% "zio-http" % zioHttpVersion,
  "dev.zio" %% "zio-interop-cats" % zioInteropCatsVersion
) ++ testDependencies

testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
