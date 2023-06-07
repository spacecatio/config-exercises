import cats.data.{EitherNec, NonEmptyChain}
import cats.syntax.all.*
import config.*
import unindent.*

case class User(email: String, password: String)
case class UserConfig(users: List[User])

type Result[A] = EitherNec[String, A]

def fail[A](message: => String): Result[A] =
  NonEmptyChain(message).raiseError[Result, A]

def decodeAsInt(config: Config): Result[Int] =
  config match
    case Config.Num(value)    => value.pure[Result]
    case config: Config.Str   => fail[Int](s"expected Num, received ${config}")
    case config: Config.Block => fail[Int](s"expected Num, received ${config}")

def decodeAsString(config: Config): Result[String] =
  config match
    case Config.Str(value)    => value.pure[Result]
    case config: Config.Num   => fail[String](s"expected Str, received ${config}")
    case config: Config.Block => fail[String](s"expected Str, received ${config}")

def getField(config: Config, name: String): Result[Config] =
  config match
    case Config.Block(fields) =>
      fields.get(name) match
        case None         => fail[Config](s"missing field '${name}': ${config}")
        case Some(config) => config.pure[Result]
    case config: Config.Str   => fail[Config](s"expected Block with field '${name}', received ${config}")
    case config: Config.Num   => fail[Config](s"expected Block with field '${name}', received ${config}")

def decodeAsUser(config: Config): Result[User] =
  (
    getField(config, "email").flatMap(decodeAsString),
    getField(config, "password").flatMap(decodeAsString),
  ).parMapN(User.apply)

def decodeAsUserConfig(config: Config): Result[UserConfig] =
  getField(config, "count").flatMap(decodeAsInt).flatMap { count =>
    val itemKeys: List[String] =
      (1 until count).map(n => s"user$n").toList

    itemKeys.traverse(key => getField(config, key).flatMap(decodeAsUser))
  }.map(UserConfig.apply)

@main def main(): Unit =
  val config: Config =
    Config.block(
      "count" -> Config.leaf(3),
      "user1" -> Config.block(
        "email" -> Config.leaf("alice@example.com"),
        "password" -> Config.leaf("password"),
      ),
      "user2" -> Config.block(
        "email" -> Config.leaf("bob@example.com"),
        "password" -> Config.leaf("supersecret"),
      ),
      "user3" -> Config.block(
        "email" -> Config.leaf("charlie@example.com"),
        "password" -> Config.leaf("opensesame"),
      ),
    )

  println(decodeAsUserConfig(config))
