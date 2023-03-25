package solutions.shitops.queries.infrastructure.ldap

import java.util.Properties
import scala.util.Try
import javax.naming.directory.InitialDirContext

class DefaultContextFactory extends ContextFactory {
  override def create(properties: Properties): Try[Context] =
    Try(new Context(new InitialDirContext(properties)))
}
