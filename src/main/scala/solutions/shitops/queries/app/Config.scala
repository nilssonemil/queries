package solutions.shitops.queries.app

import com.comcast.ip4s.Host
import com.comcast.ip4s.Port
import com.typesafe.config.ConfigFactory

import javax.xml.crypto.Data

trait Config {
  val server: ServerConfig
  val ldap: LdapConfig
  val db: DatabaseConfig
  val security: SecurityConfig
}

case class ServerConfig(address: Host, port: Port)
case class LdapConfig(uri: String)
case class DatabaseConfig(uri: String, user: String, password: String, poolSize: Int)
case class SecurityConfig(secretKey: String, tokenExpirationInSeconds: Int)

class DefaultConfig(
    val server: ServerConfig,
    val ldap: LdapConfig,
    val db: DatabaseConfig,
    val security: SecurityConfig,
) extends Config

object Config {
  def fromEnvironment(): Config = {
    val config                   = ConfigFactory.load()
    val serverAddress            = Host.fromString(config.getString("queries.server.address")).get
    val serverPort               = Port.fromInt(config.getInt("queries.server.port")).get
    val databaseUri              = config.getString("queries.db.uri")
    val databaseUser             = config.getString("queries.db.user")
    val databasePass             = config.getString("queries.db.password")
    val databasePoolSize         = config.getInt("queries.db.poolSize")
    val ldapUri                  = config.getString("queries.ldap.uri")
    val secretKey                = config.getString("queries.security.secretKey")
    val tokenExpirationInSeconds = config.getInt("queries.security.expirationInSeconds")
    return new DefaultConfig(
      server = ServerConfig(serverAddress, serverPort),
      ldap = LdapConfig(ldapUri),
      db = DatabaseConfig(databaseUri, databaseUser, databasePass, databasePoolSize),
      security = SecurityConfig(secretKey, tokenExpirationInSeconds),
    )
  }
}
