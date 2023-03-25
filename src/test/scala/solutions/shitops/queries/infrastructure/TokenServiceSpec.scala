package solutions.shitops.queries.infrastructure

import solutions.shitops.queries.BaseSpec
import solutions.shitops.queries.core.Domain.Identity

import solutions.shitops.queries.infrastructure.{Token, TokenService}
import solutions.shitops.queries.core.Domain
class TokenServiceSpec extends BaseSpec {
  val service = new TokenService("secret", 10000)

  "generateToken" - {
    val identity = Identity("tom.hanks")
    val token    = service.generateToken(identity)

    "token is a Token" in {
      token shouldBe a[Token]
    }

    "token can be verified" in {
      service.verifyIdentity(token).value shouldBe identity
    }
  }
}
