package dbPkg

import org.jooq.SQLDialect
import org.jooq.generated.tables.Users.USERS
import org.jooq.impl.DSL
import java.sql.Connection
import java.sql.Timestamp

data class User(
    val id: Int,
    val email: String,
    val encryptedPassword: String,
    val createdAt: Timestamp,
    val updatedAt: Timestamp
)

sealed class CreateUserResult
data class UserCreated(val user: User) : CreateUserResult()
object EmailAlreadyTaken : CreateUserResult()

class Db(private val conn: Connection) {

  private val create = DSL.using(conn, SQLDialect.POSTGRES_9_5)

  private fun now() = Timestamp(System.currentTimeMillis().toLong())

  fun findUserByEmail(emailAnyCase: String): User? {
    return create
        .select(USERS.ID, USERS.EMAIL, USERS.ENCRYPTED_PASSWORD, USERS.CREATED_AT, USERS.UPDATED_AT)
        .from(USERS)
        .where(USERS.EMAIL.eq(emailAnyCase.toLowerCase()))
        .fetchOneInto(User::class.java)
  }

  fun createUser(emailAnyCase: String, encryptedPassword: String): CreateUserResult {
    val email = emailAnyCase.toLowerCase()

    val user = try {
      create.insertInto(USERS,
          USERS.EMAIL,
          USERS.ENCRYPTED_PASSWORD,
          USERS.CREATED_AT,
          USERS.UPDATED_AT)
          .values(email, encryptedPassword, now(), now())
          .returning(USERS.ID,
              USERS.EMAIL,
              USERS.ENCRYPTED_PASSWORD,
              USERS.CREATED_AT,
              USERS.UPDATED_AT)
          .fetchOne()
          .into(User::class.java)
    } catch (e: org.jooq.exception.DataAccessException) {
      val message = e.message ?: ""
      if (message.contains("ERROR: duplicate key value violates unique constraint \"idx_users_email\"")) {
        return EmailAlreadyTaken
      } else {
        throw e;
      }
    }
    return UserCreated(user)
  }
}
