package solutions.shitops.queries.app

import doobie.hikari.HikariTransactor
import org.http4s.AuthedRoutes
import org.http4s.Request
import org.http4s.EntityDecoder
import solutions.shitops.queries.core.UserRepository
import cats.effect.IO
import solutions.shitops.queries.core.Domain.{Identity, User}
import org.http4s.dsl.io._

import io.circe.generic.auto._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe._
import io.circe.Encoder
import _root_.io.circe.literal._

class UserService(userRepository: UserRepository) {

  implicit val userEncoder: Encoder[User] = Encoder.instance { (user: User) =>
    json"""{
      "id": ${user.identity.value},
      "avatar": ${user.avatar}
    }"""
  }

  object Routes {
    val privateRoutes: AuthedRoutes[Identity, IO] =
      AuthedRoutes.of { case req @ GET -> Root / "users" / "me" as identity =>
        for {
          user     <- userRepository.findByIdentity(identity)
          response <- user match {
            case None       => NotFound("lol, how did this happen")
            case Some(user) => Ok(user)
          }
        } yield response
      }
  }
}
