import config.*
import unindent.*

case class SystemConfig(server: ServerConfig, client: ClientConfig)
case class ServerConfig(version: String, host: String, port: Int)
case class ClientConfig(version: String, browser: String)

object SystemConfig:
  given ConfigDecoder[SystemConfig] =
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

  println(ConfigDecoder.decode[SystemConfig](config))
