package solutions.shitops.queries.infrastructure.middleware

import cats.data.{EitherT, Kleisli, OptionT}
import cats.effect.IO
import org.http4s.{Request, _}
import org.http4s.headers.Authorization
import solutions.shitops.queries.core.Domain._
import solutions.shitops.queries.core.UserRepository
import solutions.shitops.queries.infrastructure.{Token, TokenService}

class AuthenticationMiddleware(
    authService: AuthenticationService,
    tokenService: TokenService,
    userRepository: UserRepository,
) {

  private case class Credentials(username: Username, password: Password)

  def basicAuth(service: AuthedRoutes[Identity, IO]): HttpRoutes[IO] =
    Kleisli { (req: Request[IO]) =>
      for {
        authHeader  <- EitherT.fromEither[IO](getAuthorizationHeader(req)).toOption
        credentials <- EitherT.fromEither[IO](getBasicCredentials(authHeader)).toOption
        identity    <- EitherT(authenticate(credentials)).toOption
        user        <- EitherT.liftF(findOrCreate(identity)).toOption
        resp        <- createResponse(service, req, user.identity)
      } yield resp
    }

  private val findOrCreate: Identity => IO[User] = identity =>
    userRepository
      .findByIdentity(identity)
      .flatMap {
        case Some(user) => IO.pure(user)
        case None       => userRepository.create(User(identity, None))
      }

  private val createResponse
      : (AuthedRoutes[Identity, IO], Request[IO], Identity) => OptionT[IO, Response[IO]] =
    (service, request, identity) => service(AuthedRequest.apply(context = identity, req = request))

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
        },
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

  private val authenticate: Credentials => IO[Either[AuthenticationError, Identity]] =
    credentials => authService.authenticate(credentials.username, credentials.password)
}
