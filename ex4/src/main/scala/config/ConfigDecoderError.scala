package config

import unindent.*

enum ConfigDecoderError:
  case Missing(path: List[String])
  case Invalid(expected: String, path: List[String])

  def at(path: String | List[String]): ConfigDecoderError =
    val prefix = path match
      case path: String       => List(path)
      case path: List[String] => path

    this match
      case Missing(path)           => Missing(prefix ++ path)
      case Invalid(expected, path) => Invalid(expected, prefix ++ path)

object ConfigDecoderError:
  def missing: ConfigDecoderError =
    ConfigDecoderError.Missing(Nil)

  def invalid(expected: String): ConfigDecoderError =
    ConfigDecoderError.Invalid(expected, Nil)
