import config.*
import unindent.*

case class SystemConfig(server: ServerConfig, client: ClientConfig)
case class ServerConfig(version: String, host: String, port: Int)
case class ClientConfig(version: String, browser: String)

type Result[A] = Either[String, A]

def decodeAsInt(config: Config): Result[Int] =
  config match
    case Config.Num(value)    => Right(value)
    case config: Config.Str   => Left(s"expected Num, received ${config}")
    case config: Config.Block => Left(s"expected Num, received ${config}")

def decodeAsString(config: Config): Result[String] =
  config match
    case Config.Str(value)    => Right(value)
    case config: Config.Num   => Left(s"expected Str, received ${config}")
    case config: Config.Block => Left(s"expected Str, received ${config}")

def getField(config: Config, name: String): Result[Config] =
  config match
    case Config.Block(fields) => fields.get(name).toRight(s"missing field '${name}': ${config}")
    case config: Config.Str   => Left(s"expected Block with field '${name}', received ${config}")
    case config: Config.Num   => Left(s"expected Block with field '${name}', received ${config}")

def decodeAsServerConfig(config: Config): Result[ServerConfig] =
  for
    version <- getField(config, "version").flatMap(decodeAsString)
    host    <- getField(config, "host").flatMap(decodeAsString)
    port    <- getField(config, "port").flatMap(decodeAsInt)
  yield ServerConfig(version, host, port)

def decodeAsClientConfig(config: Config): Result[ClientConfig] =
  for
    version <- getField(config, "version").flatMap(decodeAsString)
    browser <- getField(config, "browser").flatMap(decodeAsString)
  yield ClientConfig(version, browser)

def decodeAsSystemConfig(config: Config): Result[SystemConfig] =
  for
    server <- getField(config, "server").flatMap(decodeAsServerConfig)
    client <- getField(config, "client").flatMap(decodeAsClientConfig)
  yield SystemConfig(server, client)

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
