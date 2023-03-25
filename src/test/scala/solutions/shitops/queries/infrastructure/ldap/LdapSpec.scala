package solutions.shitops.queries.infrastructure.ldap

import cats.effect.IO
import solutions.shitops.queries.BaseSpec
import solutions.shitops.queries.app.LdapConfig
import solutions.shitops.queries.core.Domain._
import solutions.shitops.queries.infrastructure.ldap.ContextFactory.InitialDirContextFactory

import javax.naming.{AuthenticationException, CommunicationException, NoInitialContextException}
import javax.naming.Context.{
  INITIAL_CONTEXT_FACTORY,
  PROVIDER_URL,
  SECURITY_CREDENTIALS,
  SECURITY_PRINCIPAL,
}
import javax.naming.directory.InitialDirContext
import scala.util.{Failure, Success}

class LdapSpec extends BaseSpec {
  private val dirContext                               = new InitialDirContext()
  private val config                                   = LdapConfig("ldap://queries.org:389")
  private val contextFactory: InitialDirContextFactory = _ => IO(Success(dirContext))
  private val service                                  = new LdapService(config, contextFactory)

  "getContext" - {
    val context = service.getContext(Username("tom.hanks"), Password("tom"))

    "context has initialDirContext" in {
      context.asserting(_.value.initialDirContext shouldBe dirContext)
    }

    "context has identity" in {
      context.asserting(_.value.identity shouldBe Identity("tom.hanks"))
    }
  }

  "createPrincipal" - {
    val principal = service.createPrincipal(Username("tom.hanks"), Password("tom"))

    "has distinguished name" in {
      principal.distinguishedName.value shouldBe "uid=tom.hanks,ou=users,dc=queries,dc=org"
    }

    "has password" in {
      principal.credentials.value shouldBe "tom"
    }
  }

  "buildProperties" - {
    val uri        = "ldap://queries.org:389"
    val principal  = service.createPrincipal(Username("tom.hanks"), Password("tom"))
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
      properties.get(PROVIDER_URL) shouldBe uri
    }

    "has no extra properties" in {
      properties.size() shouldBe 4
    }
  }

  "initializeDirContext" - {
    val principal  = service.createPrincipal(Username("tom.hanks"), Password("tom"))
    val properties = service.buildProperties(principal, config)
    val context    = service.initializeDirContext(properties, contextFactory)

    "context created" in {
      context.asserting(_ shouldBe Right(dirContext))
    }

    "Returns AuthenticationError on thrown exceptions" - {
      val withError: Exception => InitialDirContextFactory = err => _ => IO(Failure(err))
      val context: InitialDirContextFactory => IO[Either[AuthenticationError, InitialDirContext]] =
        service.initializeDirContext(properties, _)

      val authError           = new AuthenticationException("invalid credentials")
      val communicationError  = new CommunicationException("unknown host")
      val initialContextError = new NoInitialContextException("class not found")

      "InvalidCredentials on AuthenticationException" in {
        context(withError(authError)).asserting(_ shouldBe Left(InvalidCredentials))
      }
      "InvalidConfiguration on CommunicationException" in {
        context(withError(communicationError))
          .asserting(_ shouldBe Left(InvalidConfiguration(communicationError)))
      }
      "InvalidConfiguration on NoInitialContextException" in {
        context(withError(initialContextError))
          .asserting(_ shouldBe Left(InvalidConfiguration(initialContextError)))
      }
    }
  }

  "createContext" - {
    val initialDirContext = new InitialDirContext()
    val context           = service.createContext(initialDirContext, Username("tom.hanks"))

    "dir context is added" in {
      context.initialDirContext shouldBe initialDirContext
    }

    "identity is added" in {
      context.identity shouldBe Identity("tom.hanks")
    }
  }
}
