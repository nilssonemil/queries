package solutions.shitops.query.infrastructure.ldap

import solutions.shitops.query.BaseSpec
import solutions.shitops.query.core.Domain
import solutions.shitops.query.core.Domain.AuthenticationError
import solutions.shitops.query.core.Domain.InvalidConfiguration
import solutions.shitops.query.core.Domain.InvalidCredentials
import solutions.shitops.query.core.Domain.Password
import solutions.shitops.query.core.Domain.UnexpectedError
import solutions.shitops.query.core.Domain.Username
import solutions.shitops.query.core.Domain._

import java.util.Properties
import javax.naming.AuthenticationException
import javax.naming.CommunicationException
import javax.naming.Context.INITIAL_CONTEXT_FACTORY
import javax.naming.Context.PROVIDER_URL
import javax.naming.Context.SECURITY_CREDENTIALS
import javax.naming.Context.SECURITY_PRINCIPAL
import javax.naming.NoInitialContextException
import javax.naming.directory.InitialDirContext
import scala.util.Failure
import scala.util.Success
import scala.util.Try

class LdapServiceSpec extends BaseSpec {
  val contextFactory = new ContextFactory {
    override def create(properties: Properties): Try[Context] =
      Success(new Context(new InitialDirContext))
  }
  val config         = LdapConfiguration("ldap://queries.org:389")
  val service        = new LdapService(config, contextFactory)
  "authenticate" - {
    val identity = service.authenticate(Username("tom.hanks"), Password("tom"))

    "return an identity" in {
      identity.value shouldBe Identity("tom.hanks")
    }

    val exceptionFactory = new ContextFactory {
      override def create(properties: Properties): Try[Context] =
        Failure(new AuthenticationException("invalid credentials"))
    }
    val exceptionService = new LdapService(config, exceptionFactory)

    "returns error if wrong password" in {
      exceptionService
        .authenticate(Username("tom.hanks"), Password("tom"))
        .left
        .value shouldBe InvalidCredentials
    }
  }

  "createSecurityPrincipal" - {
    val securityPrincipal = service.createSecurityPrincipal(Username("tom.hanks"), Password("tom"))

    "has correct username" in {
      securityPrincipal.username shouldBe Username("tom.hanks")
    }

    "has correct password" in {
      securityPrincipal.password shouldBe Password("tom")
    }

    "has correct dn" in {
      securityPrincipal.distinguishedName shouldBe "uid=tom.hanks,ou=users,dc=queries,dc=org"
    }

    "has correct credentials" in {
      securityPrincipal.credentials shouldBe "tom"
    }
  }

  "buildProperties" - {
    val principal  = SecurityPrincipal(Username("tom.hanks"), Password("tom"))
    val properties = service.buildProperties(principal, config)

    "has security principal" in {
      properties.get(SECURITY_PRINCIPAL) shouldBe "uid=tom.hanks,ou=users,dc=queries,dc=org"
    }

    "has security credentials" in {
      properties.get(SECURITY_CREDENTIALS) shouldBe "tom"
    }

    "has initial context factory" in {
      properties.get(INITIAL_CONTEXT_FACTORY) shouldBe "com.sun.jndi.ldap.LdapCtxFactory"
    }

    "has ldap provider url" in {
      properties.get(PROVIDER_URL) shouldBe "ldap://queries.org:389"
    }
    "has no extra properties" in {
      properties.size() shouldBe 4
    }
  }

  "initializeContext" - {
    val contextFactory = new ContextFactory {
      override def create(properties: Properties): Try[Context] =
        Success(new Context(new InitialDirContext()))
    }
    val properties     = service
      .buildProperties(SecurityPrincipal(Username("tom.hanks"), Password("tom")), config)

    "successful creation returns context" in {
      service.initializeContext(contextFactory, properties).value shouldBe a[Context]
    }

    val exceptionFactory: Throwable => ContextFactory                      =
      t =>
        new ContextFactory {
          override def create(properties: Properties): Try[Context] = Failure(t)
        }
    val createWithError: Throwable => Either[AuthenticationError, Context] =
      cause => service.initializeContext(exceptionFactory(cause), properties)

    "AuthenticationException returns InvalidCredentials" in {
      createWithError(
        new AuthenticationException("invalid credentials")
      ).left.value shouldBe InvalidCredentials
    }

    "CommunicationException" - {
      val error  = new CommunicationException("unknown host")
      val actual = createWithError(error)

      "returns InvalidConfiguration with cause" in {
        actual.left.value shouldBe InvalidConfiguration(error)
      }
    }

    "NoInitialContextException" - {
      val error  = new NoInitialContextException("class not found")
      val actual = createWithError(error)

      "returns InvalidConfiguration with cause" in {
        actual.left.value shouldBe InvalidConfiguration(error)
      }
    }

    "RuntimeException" - {
      val error  = new RuntimeException("unexpected error")
      val actual = createWithError(error)

      "returns UnexpectedError with cause" in {
        actual.left.value shouldBe UnexpectedError(error)
      }
    }
  }
}
