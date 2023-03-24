package solutions.shitops.query.infrastructure.ldap

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
import javax.naming.directory.SearchControls
import scala.util.Failure
import scala.util.Success
import scala.util.Try

case class LdapConfiguration(providerUrl: String)
case class SecurityPrincipal(username: Username, password: Password) {
  val distinguishedName = s"uid=${username.value},ou=users,dc=queries,dc=org"
  val credentials       = password.value
}

class LdapService(config: LdapConfiguration, contextFactory: ContextFactory)
    extends AuthenticationService {

  private val initialContextFactory = "com.sun.jndi.ldap.LdapCtxFactory"

  override def authenticate(
      username: Username,
      password: Password
  ): Either[AuthenticationError, Identity] = {
    val principal  = createSecurityPrincipal(username, password)
    val properties = buildProperties(principal, config)
    val context    = initializeContext(contextFactory, properties)
    context.map(_ => Identity(username.value))
  }

  val createSecurityPrincipal: (Username, Password) => SecurityPrincipal = (username, password) =>
    SecurityPrincipal(username, password)

  val buildProperties: (SecurityPrincipal, LdapConfiguration) => Properties = (principal, config) =>
    {
      val props = new Properties()
      props.put(SECURITY_PRINCIPAL, principal.distinguishedName)
      props.put(SECURITY_CREDENTIALS, principal.credentials)
      props.put(PROVIDER_URL, config.providerUrl)
      props.put(INITIAL_CONTEXT_FACTORY, initialContextFactory)
      props
    }

  val initializeContext: (ContextFactory, Properties) => Either[AuthenticationError, Context] =
    (factory, props) =>
      factory.create(props) match {
        case Success(ctx)                          => Right(ctx)
        case Failure(e: AuthenticationException)   => Left(InvalidCredentials)
        case Failure(e: NoInitialContextException) => Left(InvalidConfiguration(e))
        case Failure(e: CommunicationException)    => Left(InvalidConfiguration(e))
        case Failure(t)                            => Left(UnexpectedError(t))
      }

}
