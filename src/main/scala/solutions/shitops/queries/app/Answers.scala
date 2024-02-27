package solutions.shitops.queries.app

import java.util.UUID
import java.time.Instant

import io.circe.generic.auto._
import io.circe.Encoder
import _root_.io.circe.literal._
import doobie.implicits._
import doobie.implicits.toSqlInterpolator
import doobie.postgres._
import doobie.postgres.implicits._
import cats.effect.IO
import doobie.hikari.HikariTransactor
import solutions.shitops.queries.core.Domain.User
import solutions.shitops.queries.core.Domain.Identity
import solutions.shitops.queries.app.Questions.Question
import org.http4s.AuthedRoutes

import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.Request
import org.http4s.QueryParamDecoder
import org.http4s.ParseFailure
import org.http4s.QueryParam
import org.http4s.ember.core.Encoder
import org.http4s.EntityDecoder
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.http4s.HttpRoutes

object Answers {
  case class Answer(key: UUID, question: UUID, author: Identity, text: String, answeredAt: Instant)

  implicit val encoder: Encoder[Answer] = Encoder.instance { (answer: Answer) =>
    json"""{
      "key": ${answer.key},
      "author": ${answer.author.value},
      "question": ${answer.question},
      "text": ${answer.text},
      "answeredAt": ${answer.answeredAt}
    }"""
  }

  case class AnswerRequest(text: String)
  implicit val answerDecoder: EntityDecoder[IO, AnswerRequest] = jsonOf[IO, AnswerRequest]
  val deserialize: Request[IO] => IO[AnswerRequest]            = req => req.as[AnswerRequest]
  val createAnswer: (AnswerRequest, Identity, UUID) => Answer  = (req, identity, question) =>
    Answer(UUID.randomUUID(), question, identity, req.text, Instant.now())

  implicit val uuidQueryParamDecoder: QueryParamDecoder[UUID] =
    QueryParamDecoder[String].emap { str =>
      try Right(UUID.fromString(str))
      catch {
        case _: IllegalArgumentException =>
          Left(ParseFailure("Invalid UUID", s"Invalid UUID: $str"))
      }
    }

  object QuestionQueryParamMatcher extends ValidatingQueryParamDecoderMatcher[UUID]("question")

  case class QuestionQueryParam(question: UUID)

  object Routes {
    val privateRoutes: HikariTransactor[IO] => AuthedRoutes[Identity, IO] = xa =>
      AuthedRoutes.of {
        case req @ POST -> Root / "answers" :? QuestionQueryParamMatcher(
              questionValidated,
            ) as identity =>
          questionValidated.fold(
            parseFailures => BadRequest("unable to parse question key"),
            question =>
              for {
                json        <- deserialize(req.req)
                answer      <- IO(createAnswer(json, identity, question))
                savedAnswer <- Repository.save(answer)(xa)
                response    <- Created(savedAnswer)
              } yield response,
          )
      }
      val publicRoutes: HikariTransactor[IO] => HttpRoutes[IO] = xa => 
        HttpRoutes.of { case req @ GET ->  Root / "answers" :? QuestionQueryParamMatcher(questionValidated) =>
          questionValidated.fold(
            parseFailures => BadRequest("unable to parse question key"),
            question => for {
              answers <- Repository.findAllByQuestion(question)(xa)
              response <- Ok(answers)
            } yield response
            )
        }
  }

  object Repository {
    type AnswerSchema = (UUID, UUID, String, String, Instant)
    private val toSchema: Answer => AnswerSchema =
      a => (
        a.key,
        a.question,
        a.author.value,
        a.text,
        a.answeredAt
      )

    private val fromSchema: AnswerSchema => Answer =
      s => Answer(s._1, s._2, Identity(s._3), s._4, s._5)

    def save(answer: Answer)(implicit xa: HikariTransactor[IO]): IO[Answer] = {
      val answerSchema = toSchema(answer)
      sql"insert into answers(id, question, author, text, answered_at) values($answerSchema)"
        .update
        .run
        .transact(xa)
        .map((_: Any) => answer)
    }

    def findAllByQuestion(question: UUID)(implicit xa: HikariTransactor[IO]): IO[List[Answer]] =
      sql"select id, question, author, text, answered_at from answers where question = ${question} order by answered_at"
        .query[AnswerSchema]
        .map(fromSchema)
        .to[List]
        .transact(xa)
  }
}
