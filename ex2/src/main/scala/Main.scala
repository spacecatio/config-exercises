import config.*
import unindent.*

case class ServerConfig(version: String, host: String, port: Int)
case class ClientConfig(version: String, browser: String)
case class SystemConfig(server: ServerConfig, client: ClientConfig)

def decodeAsSystemConfig(config: Config): SystemConfig =
  ???

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
