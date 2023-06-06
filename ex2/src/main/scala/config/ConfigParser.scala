package config

import cats.parse.*
import cats.Functor
import cats.parse.strings.Json
import cats.syntax.all.*

object ConfigParser:
  def parse(input: String): Either[String, Config] =
    document.parseAll(input).left.map(error => error.toString)

  def unsafeParse(input: String): Config =
    document.parseAll(input) match
      case Left(error)  => throw new Exception(error.toString)
      case Right(value) => value

  private lazy val document: Parser[Config] =
    block(0)

  private def block(minIndent: Int): Parser[Config] =
    Parser.repSep(assignment(minIndent), optSpace ~ break).map(pairs => Config.Block(pairs.toList.toMap))

  private def assignment(minIndent: Int): Parser[(String, Config)] =
    lazy val singleRhs: Parser[Config] =
      (optSpace ~ Parser.char('=') ~ optSpace).with1 *> (str | num)

    lazy val blockRhs: Parser[Config] =
      (optSpace ~ Parser.char(':') ~ optSpace ~ break).with1 *> block(minIndent + 1)

    for
      _ <- skipIndent(minIndent).with1
      k <- ident
      v <- singleRhs | blockRhs
    yield (k, v)

  private lazy val str: Parser[Config] =
    Json.delimited.parser.map(Config.Str.apply)

  private lazy val num: Parser[Config] =
    Numbers.digits.map(_.toInt).map(Config.Num.apply)

  private lazy val ident: Parser[String] =
    Parser.charsWhile(char => char.isLetter | char.isDigit | char == '_')

  private def skipIndent(minIndent: Int): Parser0[Unit] =
    Parser.charIn(' ', '\t').rep0(minIndent).filter(list => list.length >= minIndent).void

  private lazy val break: Parser[Unit] =
    Parser.charIn('\r', '\n').rep.void

  private lazy val optSpace: Parser0[Unit] =
    Parser.charIn(' ', '\t').rep0.void
