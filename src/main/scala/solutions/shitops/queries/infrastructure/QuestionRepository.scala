package solutions.shitops.queries.infrastructure

import doobie.ConnectionIO
import doobie.implicits.toSqlInterpolator
import doobie.postgres._
import doobie.postgres.implicits._

import java.util.UUID

object QuestionRepository {
  case class Question(id: UUID, title: String)

  def getQuestions: ConnectionIO[List[Question]] =
    sql"select id, title from questions".query[Question].to[List]
}
