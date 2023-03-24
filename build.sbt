name         := "query"
version      := "0.1.0-SNAPSHOT"
scalaVersion := "2.13.10"
val http4sVersion = "0.23.18"
val circeVersion  = "0.14.5"
val doobieVersion = "1.0.0-RC1"
libraryDependencies ++= Seq(
  "org.http4s"           %% "http4s-ember-server"           % http4sVersion,
  "org.http4s"           %% "http4s-dsl"                    % http4sVersion,
  "org.http4s"           %% "http4s-circe"                  % http4sVersion,
  "io.circe"             %% "circe-generic"                 % circeVersion,
  "org.tpolecat"         %% "doobie-core"                   % doobieVersion,
  "org.tpolecat"         %% "doobie-hikari"                 % doobieVersion,
  "org.tpolecat"         %% "doobie-postgres"               % doobieVersion,
  "ch.qos.logback"        % "logback-classic"               % "1.4.6",
  "com.github.jwt-scala" %% "jwt-circe"                     % "9.2.0",
  // Test dependencies
  "org.scalatest"        %% "scalatest-freespec"            % "3.2.15"     % Test,
  "org.typelevel"        %% "cats-effect-testing-scalatest" % "1.4.0"      % Test,
  "io.circe"             %% "circe-literal"                 % circeVersion % Test,
)
