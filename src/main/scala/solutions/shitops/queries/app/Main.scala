package solutions.shitops.queries.app

import cats.effect.IO
import cats.effect._
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import org.http4s.ember.server._

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
  override def run(args: List[String]): IO[ExitCode]         =
    transactor.use { xa =>
      EmberServerBuilder
        .default[IO]
        .withHost(config.server.address)
        .withPort(config.server.port)
        .withHttpApp(new App(config, xa).build)
        .build
        .useForever
        .as(ExitCode.Success)
    }
}
