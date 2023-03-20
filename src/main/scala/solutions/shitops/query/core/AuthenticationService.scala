package solutions.shitops.query.core

import solutions.shitops.query.core.Domain.UserId

trait AuthenticationService {
  def authenticate(username: String, password: String): Option[UserId]
}
