package solutions.shitops.queries.app

import io.circe.Encoder
import _root_.io.circe.literal._
import cats.effect.IO
import doobie.hikari.HikariTransactor
import doobie.implicits._
import doobie.implicits.toSqlInterpolator
import doobie.postgres._
import doobie.postgres.implicits._
import io.circe.generic.auto._
import org.http4s.AuthedRoutes
import org.http4s.EntityDecoder
import org.http4s.HttpRoutes
import org.http4s.Request
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe._
import org.http4s.dsl.io._
import solutions.shitops.queries.core.Domain.Identity
import solutions.shitops.queries.core.Domain.User

import java.util.UUID

object Questions {

  case class QuestionRequest(summary: String, description: String)
  implicit val questionDecoder: EntityDecoder[IO, QuestionRequest] = jsonOf[IO, QuestionRequest]

  case class Question(key: UUID, questioner: User, summary: String, description: String)

  implicit val encoder: Encoder[Question] = Encoder.instance { (question: Question) =>
    json"""{
           "key": ${question.key},
           "questioner": ${question.questioner.identity.value},
           "summary": ${question.summary},
           "description": ${question.description}
           }"""
  }

  val deserialize: Request[IO] => IO[QuestionRequest] =
    req => req.as[QuestionRequest]

  val createQuestion: (QuestionRequest, Identity) => Question =
    (req, identity) => Question(UUID.randomUUID(), User(identity), req.summary, req.description)

  object Routes {
    val privateRoutes: HikariTransactor[IO] => AuthedRoutes[Identity, IO] = xa =>
      AuthedRoutes.of { case req @ POST -> Root / "questions" as identity =>
        for {
          json          <- deserialize(req.req)
          question      <- IO(createQuestion(json, identity))
          savedQuestion <- Repository.save(question)(xa)
          response      <- Created(savedQuestion)
        } yield response
      }
    val publicRoutes: HikariTransactor[IO] => HttpRoutes[IO]              = xa =>
      HttpRoutes.of { case req @ GET -> Root / "questions" =>
        for {
          questions <- Repository.getAll()(xa)
          response  <- Ok(questions)
        } yield response
      }
  }

  object Repository {
    case class QuestionSchema(id: UUID, questioner: String, summary: String, description: String) {
      def toQuestion = Question(
        key = id,
        questioner = User(Identity(questioner)),
        summary = summary,
        description = description,
      )
    }
    object QuestionSchema                                                                         {
      def fromQuestion(question: Question) = QuestionSchema(
        id = question.key,
        questioner = question.questioner.identity.value,
        summary = question.summary,
        description = question.description,
      )
    }

    def save(question: Question)(implicit xa: HikariTransactor[IO]): IO[Question] = {
      val questionSchema = QuestionSchema.fromQuestion(question)
      sql"insert into questions (id, questioner, summary, description) values ($questionSchema)"
        .update
        .run
        .transact(xa)
        .map((_: Any) => question)
    }

    def getAll()(implicit xa: HikariTransactor[IO]): IO[List[Question]] =
      sql"select id, questioner, summary, description from questions"
        .query[QuestionSchema]
        .to[List]
        .map(_.map(_.toQuestion))
        .transact(xa)
  }

}
