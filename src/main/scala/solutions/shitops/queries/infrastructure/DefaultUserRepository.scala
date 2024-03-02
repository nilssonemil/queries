package solutions.shitops.queries.infrastructure

import cats.effect.IO
import doobie.hikari.HikariTransactor
import doobie.implicits._
import solutions.shitops.queries.core.Domain.{Identity, User}
import solutions.shitops.queries.core.UserRepository
import java.net.URI

class DefaultUserRepository(xa: HikariTransactor[IO]) extends UserRepository {

  type UserSchema = (String, Option[String])

  val toSchema: User => UserSchema = user => (user.identity.value, user.avatar.map(_.toString()))

  val fromSchema: UserSchema => User = { case (id: String, avatar: Option[String]) =>
    User(Identity(id), avatar.map(new URI(_)))
  }

  override def findByIdentity(identity: Identity): IO[Option[User]] =
    sql"select id, avatar from users where id=${identity.value}"
      .query[UserSchema]
      .map(fromSchema)
      .option
      .transact(xa)

  override def getAll(): IO[List[User]] = ???

  override def create(user: User): IO[User] = {
    val userSchema = toSchema(user)
    sql"insert into users (id) values ($userSchema)".update.run.transact(xa).map(_ => user)
  }

}
