package solutions.shitops.query.app

import cats.effect.IO
import io.circe.generic.auto._
import org.http4s.HttpRoutes
import cats.effect._
import cats.implicits.toSemigroupKOps
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.dsl.io._
import com.comcast.ip4s._
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import org.http4s.ember.server._
import org.http4s.implicits._
import org.http4s.server.Router
import solutions.shitops.query.infrastructure.{QuestionRepository, UserRepository}
import doobie.implicits._

case class Question(title: String)

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    transactor.use { xa =>
      EmberServerBuilder
        .default[IO]
        .withHost(ipv4"0.0.0.0")
        .withPort(port"8080")
        .withHttpApp(httpApp(xa))
        .build
        .useForever
        .as(ExitCode.Success)
    }
  }

  private val transactor: Resource[IO, HikariTransactor[IO]] = for {
    ce <- ExecutionContexts.fixedThreadPool[IO](32)
    xa <- HikariTransactor.newHikariTransactor[IO](
      "org.postgresql.Driver",
      "jdbc:postgresql:queries", // database
      "postgres", // user
      "postgres", // password
      ce,
    )
  } yield xa

  private def userService(transactor: HikariTransactor[IO]) = HttpRoutes.of[IO] {
    case GET -> Root / "users" =>
      UserRepository.getUsers.transact(transactor).flatMap(Ok(_))
  }


  private def questionService(transactor: HikariTransactor[IO]) = HttpRoutes.of[IO] {
    case GET -> Root / "questions" => {
      QuestionRepository.getQuestions.transact(transactor).flatMap(Ok(_))
    }
  }
  private def services(transactor: HikariTransactor[IO]) =
    questionService(transactor) <+> userService(transactor)

  private def httpApp(transactor: HikariTransactor[IO]) =
    Router("/" -> services(transactor)).orNotFound
}
