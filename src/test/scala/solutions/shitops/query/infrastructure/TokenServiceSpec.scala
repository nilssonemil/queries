package solutions.shitops.query.infrastructure

import solutions.shitops.query.BaseSpec
import solutions.shitops.query.core.Domain.Identity

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
