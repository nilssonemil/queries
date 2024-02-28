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
import java.time.Instant
import solutions.shitops.queries.app.Answers.Answer
import java.net.URI

object Questions {

  case class QuestionRequest(summary: String, description: String)
  implicit val questionDecoder: EntityDecoder[IO, QuestionRequest] = jsonOf[IO, QuestionRequest]

  case class Question(
      key: UUID,
      answers: List[Answer],
      author: User,
      summary: String,
      description: String,
  )

  // TODO: Conditionally include avatar / make sure that Option.None is sent gracefully
  implicit val userEncoder: Encoder[User] = Encoder.instance { (user: User) =>
    json"""{
      "id": ${user.identity.value},
      "avatar": ${user.avatar}
    }"""
  }

  implicit val encoder: Encoder[Question] = Encoder.instance { (question: Question) =>
    json"""{
           "key": ${question.key},
           "answers": ${question.answers},
           "author": ${question.author},
           "summary": ${question.summary},
           "description": ${question.description}
           }"""
  }

  val deserialize: Request[IO] => IO[QuestionRequest] =
    req => req.as[QuestionRequest]

  val createQuestion: (QuestionRequest, User) => Question =
    (req, user) => Question(UUID.randomUUID(), List(), user, req.summary, req.description)

  object Routes {
    val privateRoutes: HikariTransactor[IO] => AuthedRoutes[Identity, IO] = xa =>
      AuthedRoutes.of { case req @ POST -> Root / "questions" as identity =>
        for {
          json          <- deserialize(req.req)
          question      <- IO(createQuestion(json, User(identity, None))) // TODO: Note that this will return to consumer as author with possibly incorrect avatar
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
    private type QuestionSchema = ( UUID, String, String, String )
    private type QuestionWithAnswers = (
        UUID, String, String, String, // question
        Option[UUID], Option[String], Option[String], Option[Instant], // answer
        Option[String], // NOTE: question author avatar
        Option[String], // NOTE: answer author avatar
    )

    /**
     * Groups all questions with the same ID together, to collect all answers in one Question
     */
    private val schemaToQuestions: List[QuestionWithAnswers] => List[Question] = schemas =>
      schemas.groupBy(_._1).values.map(schemaToQuestion).toList

    private val schemaToQuestion: List[QuestionWithAnswers] => Question = schemas => {
      val answers: List[Answer] = schemas.flatMap {
        case (questionId, _, _, _, Some(answerId), Some(answerAuthor), Some(answerText), Some(answeredAt), _, avatar) =>
          Some(Answer(answerId, questionId, User(Identity(answerAuthor), avatar.map(new URI(_))), answerText, answeredAt))
        case _ => None
      }
      val (id, author, summary, description, _, _, _, _, avatar, _) = schemas.head
      Question(id, answers, User(Identity(author), avatar.map(new URI(_))), summary, description)
    }

    def getAll()(implicit xa: HikariTransactor[IO]) : IO[List[Question]] = {
      sql"""
        SELECT
          q.id, q.author, q.summary, q.description,
          a.id, a.author, a.text, a.answered_at,
          qu.avatar, au.avatar
        FROM
          questions q
        LEFT JOIN
          answers a
        ON
          q.id = a.question
        LEFT JOIN
          users qu
        ON
          q.author = qu.id
        LEFT JOIN
          users au
        ON
          a.author = au.id"""
        .query[QuestionWithAnswers] // TODO: Note that all answers has to be fetched as well with author
        .to[List]
        .map(schemaToQuestions)
        .transact(xa)
    }

    private val toSchema: Question => QuestionSchema =
      question => (
        question.key,
        question.author.identity.value,
        question.summary,
        question.description,
      )

    def save(question: Question)(implicit xa: HikariTransactor[IO]): IO[Question] = {
      val schema = toSchema(question)
      sql"""
        INSERT INTO
          questions (id, author, summary, description)
        VALUES
          ($schema)
        RETURNING
          (SELECT avatar FROM users WHERE id = ${schema._2})
        """
        .query[Option[String]] // TODO: Do we need to fetch answers here too? Or can we assume it is right?
        .unique
        .transact(xa)
        .map(avatar => question.copy(author = question.author.copy(avatar = avatar.map(new URI(_)))))
    }
  }
}
