package config

import cats.syntax.all.*

enum Config:
  case Block(fields: Map[String, Config])
  case Str(value: String)
  case Num(value: Int)

  def get(path: String | List[String]): ConfigDecoder.Result[Config] =
    val expanded: List[String] =
      path match
        case path: String       => List(path)
        case path: List[String] => path

    expanded match
      case head :: tail =>
        this match
          case Block(fields) =>
            fields.get(head) match
              case None         => ConfigDecoder.missingError
              case Some(config) => config.get(tail).prefix(head)

          case config =>
            ConfigDecoder.invalidError[Config](s"block with field '${head}'")

      case Nil =>
        this.pure[ConfigDecoder.Result]

object Config:
  def block(fields: (String, Config)*): Block =
    Block(fields.toMap)

  def leaf(value: String | Int): Config =
    value match
      case value: String => Str(value)
      case value: Int    => Num(value)
