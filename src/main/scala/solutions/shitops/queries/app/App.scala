package solutions.shitops.queries.app

import cats.effect.IO
import doobie.hikari.HikariTransactor
import solutions.shitops.queries.infrastructure.DefaultUserRepository
import solutions.shitops.queries.infrastructure.TokenService
import solutions.shitops.queries.infrastructure.middleware.AuthenticationMiddleware
import solutions.shitops.queries.core.Domain._
import org.http4s.server.Router
import org.http4s.AuthedRoutes
import org.http4s.HttpRoutes
import solutions.shitops.queries.infrastructure.Token
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import io.circe.generic.auto._
import cats.implicits.toSemigroupKOps
import doobie.implicits._
import org.http4s.dsl.io._
import solutions.shitops.queries.infrastructure.QuestionRepository
import solutions.shitops.queries.infrastructure.ldap.{ContextFactory, LdapService} // TODO: Remove
import org.http4s.server.middleware._

class App(config: Config, transactor: HikariTransactor[IO]) {

  private val userRepository        = new DefaultUserRepository(transactor)
  private val authenticationService =
    new LdapService(config.ldap, ContextFactory.dirContextFactory)
  private val tokenService          = new TokenService(config.security)
  private val middleware            =
    new AuthenticationMiddleware(authenticationService, tokenService, userRepository)

  def build() = Router("/" -> CORS(routes(transactor))).orNotFound

  case class TokenResponse(token: String)

  private val tokenRoutes =
    AuthedRoutes.of[Identity, IO] { case POST -> Root / "token" as identity =>
      val token: Token            = tokenService.generateToken(identity)
      val response: TokenResponse = TokenResponse(token.encoded)
      Ok(response)
    }

  private def publicRoutes(transactor: HikariTransactor[IO]): HttpRoutes[IO] =
    Questions.Routes.publicRoutes(transactor)

  private def privateRoutes(transactor: HikariTransactor[IO]): AuthedRoutes[Identity, IO] =
    Questions.Routes.privateRoutes(transactor)

  private def routes(transactor: HikariTransactor[IO]): HttpRoutes[IO] =
    middleware.basicAuth(tokenRoutes) <+>
      publicRoutes(transactor) <+>
      middleware.tokenAuth(privateRoutes(transactor))
}
