package solutions.shitops.queries.infrastructure

import pdi.jwt.Jwt
import pdi.jwt.JwtAlgorithm
import pdi.jwt.JwtCirce
import pdi.jwt.JwtClaim
import solutions.shitops.queries.core.Domain.AuthenticationError
import solutions.shitops.queries.core.Domain.Identity
import solutions.shitops.queries.core.Domain.InvalidCredentials
import solutions.shitops.queries.app.SecurityConfig

import java.time.Instant
import scala.util.Failure
import scala.util.Success
import scala.util.Try

case class TokenConfiguration(secretKey: String, expirationInSeconds: Int)
case class Token(encoded: String)

class TokenService(config: SecurityConfig) {
  private val alg = JwtAlgorithm.HS512

  private val generateClaim: Identity => JwtClaim = identity =>
    JwtClaim(
      subject = Some(identity.value),
      expiration = Some(Instant.now.plusSeconds(config.tokenExpirationInSeconds).getEpochSecond),
      issuedAt = Some(Instant.now.getEpochSecond),
    )

  private val encode: JwtClaim => Token =
    claim => Token(JwtCirce.encode(claim, config.secretKey, alg))

  private val decode: Token => Either[AuthenticationError, JwtClaim] =
    token =>
      JwtCirce.decode(token.encoded, config.secretKey, Seq(alg)) match {
        case Success(claim) => Right(claim)
        case Failure(t)     => Left(InvalidCredentials)
      }

  private val getIdentity: JwtClaim => Either[AuthenticationError, Identity] = claim =>
    claim.subject match {
      case Some(s) => Right(Identity(s))
      case None    => Left(InvalidCredentials)
    }

  val generateToken: Identity => Token =
    generateClaim andThen encode

  val verifyIdentity: Token => Either[AuthenticationError, Identity] =
    token =>
      for {
        claim    <- decode(token)
        identity <- getIdentity(claim)
      } yield identity
}
