package config

enum Config:
  case Block(fields: Map[String, Config])
  case Str(value: String)
  case Num(value: Int)

  def get(path: String | List[String]): Option[Config] =
    val expanded: List[String] =
      path match
        case path: String       => List(path)
        case path: List[String] => path

    expanded match
      case head :: tail =>
        this match
          case Block(fields) => fields.get(head).flatMap(_.get(tail))
          case _             => None

      case Nil =>
        Some(this)

object Config:
  def block(fields: (String, Config)*): Block =
    Block(fields.toMap)

  def leaf(value: String | Int): Config =
    value match
      case value: String => Str(value)
      case value: Int    => Num(value)
