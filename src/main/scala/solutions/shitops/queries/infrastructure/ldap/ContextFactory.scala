package solutions.shitops.queries.infrastructure.ldap

import java.util.Properties
import javax.naming.directory.InitialDirContext
import scala.util.Try

class Context(val ctx: InitialDirContext)

trait ContextFactory {
  def create(properties: Properties): Try[Context]
}
