import config.*
import unindent.*

case class User(email: String, password: String)
case class UserConfig(users: List[User])

def decodeAsUserConfig(config: Config): UserConfig =
  ???

@main def main(): Unit =
  val config: Config =
    ConfigParser.unsafeParse(
      i"""
      users:
        count = 3
        user1:
          email    = "alice@example.com"
          password = "password"
        user2:
          email    = "bob@example.com"
          password = "supersecret"
        user3:
          email    = "charlie@example.com"
          password = "opensesame"
      """
    )

  println(decodeAsSystemConfig(config))
