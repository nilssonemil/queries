package solutions.shitops.queries.core

import cats.effect.IO

object Domain {
  case class Username(value: String)
  case class Password(value: String)
  case class Identity(value: String)
  case class User(identity: Identity)
  sealed trait AuthenticationError
  case object InvalidCredentials                    extends AuthenticationError
  case class InvalidConfiguration(cause: Throwable) extends AuthenticationError
  case class UnexpectedError(cause: Throwable)      extends AuthenticationError

  trait AuthenticationService {
    def authenticate(
        username: Username,
        password: Password,
    ): IO[Either[AuthenticationError, Identity]]
  }
}
