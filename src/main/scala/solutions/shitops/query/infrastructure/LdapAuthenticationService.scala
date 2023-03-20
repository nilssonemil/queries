package solutions.shitops.query.infrastructure

import solutions.shitops.query.core.{AuthenticationService, Domain}

import java.util.Properties
import javax.naming.Context
import javax.naming.directory.{InitialDirContext, SearchControls}
import scala.util.{Failure, Success, Try}

class LdapAuthenticationService(val ldapUrl: String)
    extends AuthenticationService {

  override def authenticate(
      username: String,
      password: String
  ): Option[Domain.UserId] =
    createContext(s"uid=$username,ou=users,dc=queries,dc=org", password)
      .map(ctx => {
        val searchControls = new SearchControls()
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE)
        searchControls.setReturningAttributes(Array[String]("uid"))
        val results = ctx.search(
          s"uid=$username,ou=users,dc=queries,dc=org",
          s"uid=$username",
          searchControls,
        )
        Domain.UserId(results.next().getAttributes.get("uid").get().toString)
      })
      .toOption

  private def createContext(
      securityPrincipal: String,
      securityCredentials: String,
  ): Either[Throwable, InitialDirContext] =
    Try {
      val props = new Properties()
      props.put(
        Context.INITIAL_CONTEXT_FACTORY,
        "com.sun.jndi.ldap.LdapCtxFactory"
      )
      props.put(Context.PROVIDER_URL, ldapUrl)
      props.put(Context.SECURITY_PRINCIPAL, securityPrincipal)
      props.put(Context.SECURITY_CREDENTIALS, securityCredentials)
      new InitialDirContext(props)
    } match {
      case Success(ctx) => Right(ctx)
      case Failure(e)   => Left(e)
    }
}
