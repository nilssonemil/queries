package solutions.shitops.queries.app

import cats.effect.IO
import cats.effect._
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import org.http4s.ember.server._
import org.http4s.server.middleware.ErrorHandling
import org.http4s.server.middleware.ErrorAction
import cats.data.Kleisli
import org.http4s.Request
import org.http4s.Response

object Main extends IOApp {
  private val config                                         = Config.fromEnvironment()
  private val transactor: Resource[IO, HikariTransactor[IO]] = for {
    ce <- ExecutionContexts.fixedThreadPool[IO](config.db.poolSize)
    xa <- HikariTransactor.newHikariTransactor[IO](
      "org.postgresql.Driver",
      config.db.uri,
      config.db.user,
      config.db.password,
      ce,
    )
  } yield xa

  val withErrorLogging = (route: Kleisli[IO, Request[IO], Response[IO]]) => ErrorHandling
    .Recover
    .total(
      ErrorAction.log(
        route,
        messageFailureLogAction = (t, msg) =>
          IO.println(msg) >>
            IO.println(t),
        serviceErrorLogAction = (t, msg) =>
          IO.println(msg) >>
            IO.println(t),
      ),
    )

  override def run(args: List[String]): IO[ExitCode] =
    transactor.use { xa =>
      EmberServerBuilder
        .default[IO]
        .withHost(config.server.address)
        .withPort(config.server.port)
        .withHttpApp(withErrorLogging(new App(config, xa).build))
        .build
        .useForever
        .as(ExitCode.Success)
    }
}
