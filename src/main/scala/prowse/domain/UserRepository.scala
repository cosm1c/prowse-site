package prowse.domain

case class User(username: String,
                firstName: String,
                lastName: String,
                email: String,
                url: String)

/* TODO: UserRepository
trait UserRepository {
  def findByUsername(username: String): Future[Option[User]]
}
*/