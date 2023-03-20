package solutions.shitops.query.infrastructure

import doobie.implicits.toSqlInterpolator

/* Needed to get map java.util.UUID to postgres */
import doobie.postgres._
import doobie.postgres.implicits._

import java.util.UUID

object QuestionRepository {
  case class Question(id: UUID, title: String)

  def getQuestions: doobie.ConnectionIO[List[Question]] =
    sql"select id, title from questions"
      .query[Question]
      .to[List]
}