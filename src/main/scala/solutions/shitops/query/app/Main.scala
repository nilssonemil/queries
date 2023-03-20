package solutions.shitops.query.app

import cats.effect.IO
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.HttpRoutes
import cats.effect._
import org.http4s._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.jsonEncoder
import org.http4s.dsl.io._
import cats.syntax.all._
import com.comcast.ip4s._
import org.http4s.ember.server._
import org.http4s.implicits._
import org.http4s.server.Router
import scala.concurrent.duration._

case class Question(title: String)

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    server
      .useForever
      .as(ExitCode.Success)

  val question: Question = Question("What does the fox say?")
  val questions: List[Question] = List(
    question,
    Question("How does the horse move?"),
    Question("How do you serialize a list of case classes?"),
  )
  val questionJson: Json = question.asJson
  val questionsJson: Json = questions.asJson

  def getQuestions: IO[List[Question]] = IO.pure(questions)

  val questionService = HttpRoutes.of[IO] {
    case GET -> Root / "questions" => {
      getQuestions.flatMap(Ok(_))
    }
  }
  val httpApp = Router("/" -> questionService).orNotFound
  val server = EmberServerBuilder
    .default[IO]
    .withHost(ipv4"0.0.0.0")
    .withPort(port"8080")
    .withHttpApp(httpApp)
    .build
}
