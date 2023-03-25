package solutions.shitops.queries.infrastructure

import doobie.ConnectionIO
import doobie.implicits.toSqlInterpolator

object UserRepository {
  case class User(id: String)
  def getUsers: ConnectionIO[List[User]] =
    sql"select id from users".query[User].to[List]
}
