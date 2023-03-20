package solutions.shitops.query.infrastructure

import java.util.Properties
import javax.naming.{Context, NamingEnumeration}
import javax.naming.directory.{InitialDirContext, SearchControls, SearchResult}
import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Success, Try}

object LdapRepository {
  case class User(uid: String, cn: String, sn: String)
  def getUsers: ListBuffer[User] = {
    // TODO: Use user credentials
    val ctx = setupContext("admin", "admin")
    ctx.map(ctx => {
      val searchControls = new SearchControls()
      searchControls.setReturningAttributes(Array[String]("uid", "cn", "sn"))
      val results: NamingEnumeration[SearchResult] = ctx.search("ou=users,dc=queries,dc=org", "cn=*", searchControls)
      val users: ListBuffer[User] = ListBuffer()
      while (results.hasMore) {
        val attributes = results.next().getAttributes
        val user = User(
          attributes.get("uid").get().toString,
          attributes.get("cn").get().toString,
          attributes.get("sn").get().toString)
        users += user
      }
      users
    }).getOrElse(ListBuffer.empty[User])
  }

  private def setupContext(cn: String, password: String): Either[Throwable, InitialDirContext] = {
    Try {
      val props = new Properties()
      props.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory")
      props.put(Context.PROVIDER_URL, "ldap://localhost:389")
      props.put(Context.SECURITY_PRINCIPAL, s"cn=$cn,dc=queries,dc=org")
      props.put(Context.SECURITY_CREDENTIALS, password)
      new InitialDirContext(props)
    }  match {
      case Success(ctx) => Right(ctx)
      case Failure(e) => Left(e)
    }
  }
}
