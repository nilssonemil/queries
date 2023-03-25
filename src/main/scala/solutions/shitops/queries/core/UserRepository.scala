package solutions.shitops.queries.core

import cats.effect.IO
import solutions.shitops.queries.core.Domain.Identity
import solutions.shitops.queries.core.Domain.User

sealed trait UserCreationError

trait UserRepository {
  def findByIdentity(identity: Identity): IO[Option[User]]
  def getAll(): IO[List[User]]
  def create(user: User): IO[User]
}
