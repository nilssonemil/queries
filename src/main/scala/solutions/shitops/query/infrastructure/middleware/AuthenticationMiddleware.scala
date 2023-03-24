package solutions.shitops.query.infrastructure.middleware

import cats.data
import cats.data.Kleisli
import cats.data.OptionT
import cats.effect.IO
import cats.effect._
import cats.implicits._
import cats.syntax.all._
import org.http4s.BasicCredentials
import org.http4s.CharsetRange.*
import org.http4s.Request
import org.http4s._
import org.http4s.dsl.impl.ResponseGenerator
import org.http4s.dsl.io._
import org.http4s.headers.Authorization
import org.http4s.server.AuthMiddleware
import org.http4s.syntax.header
import solutions.shitops.query.core.Domain._
import solutions.shitops.query.infrastructure.Token
import solutions.shitops.query.infrastructure.TokenService

import scala.ref.ReferenceQueue

class AuthenticationMiddleware(authService: AuthenticationService, tokenService: TokenService) {

  case class Credentials(username: Username, password: Password)

  def basicAuth(service: AuthedRoutes[Identity, IO]): HttpRoutes[IO] =
    Kleisli { (req: Request[IO]) =>
      val identity = for {
        authHeader  <- getAuthorizationHeader(req)
        credentials <- getBasicCredentials(authHeader)
        identity    <- authenticate(credentials)
      } yield identity

      val response                            = identity.toOption.map(respond(service, req, _))
      val continue: OptionT[IO, Response[IO]] = OptionT.none

      response.getOrElse(continue)
    }

  val respond: (AuthedRoutes[Identity, IO], Request[IO], Identity) => OptionT[IO, Response[IO]] =
    (service, request, identity) => {
      val authedRequest = AuthedRequest.apply(context = identity, req = request)
      service(authedRequest)
    }

  def tokenAuth(service: AuthedRoutes[Identity, IO]): HttpRoutes[IO] =
    Kleisli { (req: Request[IO]) =>
      val identity = for {
        authHeader <- getAuthorizationHeader(req)
        token      <- getToken(authHeader)
        identity   <- tokenService.verifyIdentity(token)
      } yield identity

      identity.fold(
        err => {
          println(s"Token Error: $err")
          val response: IO[Response[IO]] = IO.pure(Response(Status.Unauthorized))
          OptionT.liftF(response)
        },
        identity => {
          val authedRequest = AuthedRequest.apply(context = identity, req = req)
          service(authedRequest)
        }
      )
    }

  private val getBasicCredentials: Authorization => Either[AuthenticationError, Credentials] =
    header =>
      header.credentials match {
        case BasicCredentials(username, password) =>
          Right(Credentials(Username(username), Password(password)))
        case _                                    => Left(InvalidCredentials)
      }

  private val getToken: Authorization => Either[AuthenticationError, Token] =
    header =>
      header.credentials match {
        case org.http4s.Credentials.Token(AuthScheme.Bearer, token) => Right(Token(token))
        case _                                                      => Left(InvalidCredentials)
      }

  private val getAuthorizationHeader: Request[IO] => Either[AuthenticationError, Authorization] =
    req =>
      req.headers.get[Authorization] match {
        case Some(header) => Right(header)
        case None         => Left(InvalidCredentials)
      }

  private val authenticate: Credentials => Either[AuthenticationError, Identity] =
    credentials => authService.authenticate(credentials.username, credentials.password)
}
