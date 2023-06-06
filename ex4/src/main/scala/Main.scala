import cats.syntax.all.*
import config.*
import unindent.*

case class ServerConfig(version: String, host: String, port: Int)

object ServerConfig:
  given ConfigDecoder[ServerConfig] = (
    ConfigDecoder.field("version").as[String],
    ConfigDecoder.field("host").as[String],
    ConfigDecoder.field("port").as[Int],
  ).parMapN(ServerConfig.apply)

case class ClientConfig(version: String, browser: String)

object ClientConfig:
  given ConfigDecoder[ClientConfig] = (
    ConfigDecoder.field("version").as[String],
    ConfigDecoder.field("browser").as[String],
  ).parMapN(ClientConfig.apply)

case class SystemConfig(server: ServerConfig, client: ClientConfig)

object SystemConfig:
  given ConfigDecoder[SystemConfig] = (
    ConfigDecoder.field("server").as[ServerConfig],
    ConfigDecoder.field("client").as[ClientConfig],
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

  println(ConfigDecoder.decode[SystemConfig](config))
