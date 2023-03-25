package solutions.shitops.queries.app

import cats.effect.IO
import cats.effect._
import cats.implicits.toSemigroupKOps
import com.comcast.ip4s._
import doobie.hikari.HikariTransactor
import doobie.implicits._
import doobie.util.ExecutionContexts
import io.circe.generic.auto._
import org.http4s.AuthedRoutes
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.dsl.io._
import org.http4s.ember.server._
import org.http4s.server.Router
import solutions.shitops.queries.infrastructure
import solutions.shitops.queries.core.Domain._
import solutions.shitops.queries.infrastructure.QuestionRepository
import solutions.shitops.queries.infrastructure.Token
import solutions.shitops.queries.infrastructure.TokenService
import solutions.shitops.queries.infrastructure.ldap.DefaultContextFactory
import solutions.shitops.queries.infrastructure.ldap.LdapConfiguration
import solutions.shitops.queries.infrastructure.ldap.LdapService
import solutions.shitops.queries.infrastructure.middleware.AuthenticationMiddleware

object Main extends IOApp {

  private val authenticationService: AuthenticationService =
    new LdapService(LdapConfiguration("ldap://localhost:389"), new DefaultContextFactory())
  private val tokenService: TokenService                   = new TokenService("secretkey", 10000)
  private val middleware = new AuthenticationMiddleware(authenticationService, tokenService)
  override def run(args: List[String]): IO[ExitCode] =
    transactor.use { xa =>
      EmberServerBuilder
        .default[IO]
        .withHost(ipv4"0.0.0.0")
        .withPort(port"8080")
        .withHttpApp(httpApp(xa))
        .build
        .useForever
        .as(ExitCode.Success)
    }

  private val transactor: Resource[IO, HikariTransactor[IO]] = for {
    ce <- ExecutionContexts.fixedThreadPool[IO](32)
    xa <- HikariTransactor.newHikariTransactor[IO](
      "org.postgresql.Driver",
      "jdbc:postgresql:queries", // database
      "postgres",                // user
      "postgres",                // password
      ce,
    )
  } yield xa

  case class TokenResponse(token: String)

  private val tokenRoutes =
    AuthedRoutes.of[Identity, IO] { case POST -> Root / "token" as identity =>
      val token: Token            = tokenService.generateToken(identity)
      val response: TokenResponse = TokenResponse(token.encoded)
      Ok(response)
    }

  private def publicRoutes(transactor: HikariTransactor[IO]) =
    HttpRoutes.of[IO] { case GET -> Root / "questions" =>
      QuestionRepository.getQuestions.transact(transactor).flatMap(Ok(_))
    }

  private def privateRoutes(
      transactor: HikariTransactor[IO],
  ): AuthedRoutes[Identity, IO] =
    AuthedRoutes.of { case POST -> Root / "questions" as identity =>
      Created(s"Welcome, ${identity.value}")
    }

  private def routes(transactor: HikariTransactor[IO]): HttpRoutes[IO] =
    middleware.basicAuth(tokenRoutes) <+>
      publicRoutes(transactor) <+>
      middleware.tokenAuth(privateRoutes(transactor))

  private def httpApp(transactor: HikariTransactor[IO]) =
    Router("/" -> routes(transactor)).orNotFound
}
