package solutions.shitops.queries.infrastructure

import cats.effect.IO
import doobie.hikari.HikariTransactor
import doobie.implicits._
import solutions.shitops.queries.core.Domain.{Identity, User}
import solutions.shitops.queries.core.UserRepository

class DefaultUserRepository(xa: HikariTransactor[IO]) extends UserRepository {

  case class UserSchema(id: String) {
    def toUser = User(Identity(id))
  }
  object UserSchema                 {
    def fromUser(user: User): UserSchema = UserSchema(user.identity.value)
  }

  override def findByIdentity(identity: Identity): IO[Option[User]] =
    sql"select id from users where id=$identity".query[UserSchema].map(_.toUser).option.transact(xa)

  override def getAll(): IO[List[User]] = ???

  override def create(user: User): IO[User] = {
    val userSchema = UserSchema.fromUser(user)
    sql"insert into users (id) values ($userSchema)".update.run.transact(xa).map(_ => user)
  }

}
