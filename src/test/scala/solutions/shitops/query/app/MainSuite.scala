package solutions.shitops.query.app

import org.scalatest.funsuite.AnyFunSuite
import solutions.shitops.query.infrastructure.LdapAuthenticationService

class MainSuite extends AnyFunSuite {
  private val authenticationService = new LdapAuthenticationService(
    "ldap://localhost:389"
  )

  test("authentication") {
    assert(authenticationService.authenticate("tom.hanks", "tom").nonEmpty)
    assert(authenticationService.authenticate("marie.curie", "marie").nonEmpty)
    assert(authenticationService.authenticate("tom.hanks", "marie").isEmpty)
    assert(authenticationService.authenticate("tom.curie", "marie").isEmpty)
  }
}
