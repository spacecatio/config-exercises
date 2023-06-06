package config

enum Config:
  case Block(fields: Map[String, Config])
  case Str(value: String)
  case Num(value: Int)

object Config:
  def block(fields: (String, Config)*): Block =
    Block(fields.toMap)

  def leaf(value: String | Int): Config =
    value match
      case value: String => Str(value)
      case value: Int    => Num(value)
