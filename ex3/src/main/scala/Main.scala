import config.*
import unindent.*
import cats.data.{EitherNec, NonEmptyChain}
import cats.syntax.all.*

case class SystemConfig(server: ServerConfig, client: ClientConfig)
case class ServerConfig(version: String, host: String, port: Int)
case class ClientConfig(version: String, browser: String)

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

def decodeAsServerConfig(config: Config): Result[ServerConfig] =
  (
    getField(config, "version").flatMap(decodeAsString),
    getField(config, "host").flatMap(decodeAsString),
    getField(config, "port").flatMap(decodeAsInt),
  ).parMapN(ServerConfig.apply)

def decodeAsClientConfig(config: Config): Result[ClientConfig] =
  (
    getField(config, "version").flatMap(decodeAsString),
    getField(config, "browser").flatMap(decodeAsString),
  ).parMapN(ClientConfig.apply)

def decodeAsSystemConfig(config: Config): Result[SystemConfig] =
  (
    getField(config, "server").flatMap(decodeAsServerConfig),
    getField(config, "client").flatMap(decodeAsClientConfig),
  ).parMapN(SystemConfig.apply)

@main def main(): Unit =
  val config: Config =
    ConfigParser.unsafeParse(
      i"""
      server:
        version = "2.1.0"
        host    = "localhost"
        port    = 8000
      client:
        version = "1.0.0"
        browser = "Firefox"
      """
    )

  println(decodeAsSystemConfig(config))
