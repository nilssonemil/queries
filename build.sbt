name := "query"
version := "0.1.0-SNAPSHOT"
scalaVersion := "2.13.10"
val http4sVersion = "0.23.18"
val circeVersion = "0.14.5"
libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-ember-server" % http4sVersion,
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-circe" % http4sVersion,
  "io.circe" %% "circe-generic" % circeVersion,

  // Test dependencies
  "io.circe" %% "circe-literal" % circeVersion % Test,
  "org.scalatest" %% "scalatest-funsuite" % "3.2.15" % Test,
)