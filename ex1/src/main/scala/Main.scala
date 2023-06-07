import config.*
import unindent.*

case class SystemConfig(server: ServerConfig, client: ClientConfig)
case class ServerConfig(version: String, host: String, port: Int)
case class ClientConfig(version: String, browser: String)

def decodeAsInt(config: Config): Option[Int] =
  config match
    case Config.Num(value) => Some(value)
    case Config.Str(_)     => None
    case Config.Block(_)   => None

def decodeAsString(config: Config): Option[String] =
  config match
    case Config.Str(value) => Some(value)
    case Config.Num(_)     => None
    case Config.Block(_)   => None

def getField(config: Config, name: String): Option[Config] =
  config match
    case Config.Block(fields) => fields.get(name)
    case Config.Str(_)        => None
    case Config.Num(_)        => None

def decodeAsServerConfig(config: Config): Option[ServerConfig] =
  for
    version <- getField(config, "version").flatMap(decodeAsString)
    host    <- getField(config, "host").flatMap(decodeAsString)
    port    <- getField(config, "port").flatMap(decodeAsInt)
  yield ServerConfig(version, host, port)

def decodeAsClientConfig(config: Config): Option[ClientConfig] =
  for
    version <- getField(config, "version").flatMap(decodeAsString)
    browser <- getField(config, "browser").flatMap(decodeAsString)
  yield ClientConfig(version, browser)

def decodeAsSystemConfig(config: Config): Option[SystemConfig] =
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
