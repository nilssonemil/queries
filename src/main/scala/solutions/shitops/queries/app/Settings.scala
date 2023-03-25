package solutions.shitops.queries.app

import com.comcast.ip4s.Host
import com.comcast.ip4s.Port
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

class Settings(config: Config) {
  val serverAddress            = Host.fromString(config.getString("queries.server.address")).get
  val serverPort               = Port.fromInt(config.getInt("queries.server.port")).get
  val databaseUri              = config.getString("queries.db.uri")
  val databaseUser             = config.getString("queries.db.user")
  val databasePass             = config.getString("queries.db.password")
  val databasePoolSize         = config.getInt("queries.db.poolSize")
  val ldapUri                  = config.getString("queries.ldap.uri")
  val secretKey                = config.getString("queries.security.secretKey")
  val tokenExpirationInSeconds = config.getInt("queries.security.expirationInSeconds")
}
object Settings                {
  def fromEnvironment(): Settings = new Settings(ConfigFactory.load())
}
