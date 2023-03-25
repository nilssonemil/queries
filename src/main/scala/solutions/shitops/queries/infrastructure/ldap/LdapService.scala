package solutions.shitops.queries.infrastructure.ldap

import cats.effect.IO
import solutions.shitops.queries.app.LdapConfig
import solutions.shitops.queries.core.Domain._
import solutions.shitops.queries.infrastructure.ldap.ContextFactory.InitialDirContextFactory

import javax.naming.Context.{
  INITIAL_CONTEXT_FACTORY,
  PROVIDER_URL,
  SECURITY_CREDENTIALS,
  SECURITY_PRINCIPAL,
}
import java.util.Properties
import javax.naming.{AuthenticationException, CommunicationException, NoInitialContextException}
import javax.naming.directory.InitialDirContext
import scala.util.{Failure, Success, Try}

case class Context(initialDirContext: InitialDirContext, identity: Identity)

object ContextFactory {
  type InitialDirContextFactory = Properties => IO[Try[InitialDirContext]]
  val dirContextFactory: InitialDirContextFactory = properties =>
    IO(Try(new InitialDirContext(properties)))
}

class LdapService(config: LdapConfig, initialDirContextFactory: InitialDirContextFactory)
    extends AuthenticationService {

  case class DistinguishedName(value: String)
  private object DistinguishedName {
    def fromUsername(username: Username): DistinguishedName = DistinguishedName(
      s"uid=${username.value},ou=users,dc=queries,dc=org",
    )
  }
  case class Principal(distinguishedName: DistinguishedName, credentials: Password)
  private object Principal         {
    def fromUsernameAndPassword(username: Username, password: Password): Principal =
      Principal(
        DistinguishedName.fromUsername(username),
        password,
      )
  }

  override def authenticate(
      username: Username,
      password: Password,
  ): IO[Either[AuthenticationError, Identity]] =
    getContext(username, password).map(_.map(_.identity))

  def getContext(
      username: Username,
      password: Password,
  ): IO[Either[AuthenticationError, Context]] = {
    val principal         = createPrincipal(username, password)
    val properties        = buildProperties(principal, config)
    val initialDirContext = initializeDirContext(properties, initialDirContextFactory)
    val context: IO[Either[AuthenticationError, Context]] = initialDirContext
      .map(either => either.map(ctx => createContext(ctx, username)))
    context
  }

  val createPrincipal: (Username, Password) => Principal = Principal.fromUsernameAndPassword

  val buildProperties: (Principal, LdapConfig) => Properties = (principal, config) => {
    val props = new Properties()
    props.put(SECURITY_PRINCIPAL, principal.distinguishedName.value)
    props.put(SECURITY_CREDENTIALS, principal.credentials.value)
    props.put(PROVIDER_URL, config.uri)
    props.put(INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory")
    props
  }

  val initializeDirContext: (Properties, InitialDirContextFactory) => IO[
    Either[AuthenticationError, InitialDirContext],
  ] = { (properties, contextFactory) =>
    contextFactory(properties).map {
      case Success(ctx)                          => Right(ctx)
      case Failure(_: AuthenticationException)   => Left(InvalidCredentials)
      case Failure(e: CommunicationException)    => Left(InvalidConfiguration(e))
      case Failure(e: NoInitialContextException) => Left(InvalidConfiguration(e))
    }
  }

  val createContext: (InitialDirContext, Username) => Context =
    (initialDirContext, username) => Context(initialDirContext, Identity(username.value))
}
