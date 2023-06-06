package config

import cats.arrow.FunctionK
import cats.data.{EitherNec, NonEmptyChain, Validated}
import cats.{~>, Applicative, FlatMap, Monad, Parallel}
import cats.syntax.all.*

/** Type class for decoding instances of `Config` as domain-specific types.
  *
  * The following code:
  *
  * ```
  * ConfigDecoder.path("foo").as[Bar]
  * ```
  *
  * defines a `ConfigDecoder[String]` that decodes path "foo" as a value of type `Bar`. The `as` method takes an implicit parameter of type
  * `ConfigDecoder[Bar]`.
  *
  * `ConfigDecoder` is a monad, which allows sequential composition of decoders. Semantics are fail-fast - if the first decoder fails,
  * subsequent decoders are not used and only one error message is returned:
  *
  * ```
  * val cellDecoder: ConfigDecoder[(String, Int)] =
  *   for
  *     col <- ConfigDecoder.path("col").as[String]
  *     row <- ConfigDecoder.path("row").as[Int]
  *   yield (col, row)
  * ```
  *
  * `ConfigDecoder` also has an instance of `Parallel` allowing the creation of cumulative decoders that try to decode multiple fields in
  * parallel and fail with the combination of all errors received:
  *
  * ```
  * val cellDecoder: ConfigDecoder[(String, Int)] =
  *   (
  *     ConfigDecoder.field("col").as[String],
  *     ConfigDecoder.field("row").as[Int],
  *   ).parTupled
  * ```
  */
trait ConfigDecoder[A]:
  import ConfigDecoder.Result

  def decode(config: Config): Result[A]

  def at(path: String | List[String]): ConfigDecoder[A] =
    ConfigDecoder.instance(_.get(path).flatMap(this.decode).prefix(path))

object ConfigDecoder:
  type Result[A] = EitherNec[ConfigDecoderError, A]

  def apply[A](using decoder: ConfigDecoder[A]): ConfigDecoder[A] =
    decoder

  def decode[A](config: Config)(using decoder: ConfigDecoder[A]): ConfigDecoder.Result[A] =
    decoder.decode(config)

  def instance[A](func: Config => Result[A]): ConfigDecoder[A] =
    new ConfigDecoder[A]:
      override def decode(config: Config): Result[A] =
        func(config)

  val identity: ConfigDecoder[Config] =
    instance(_.pure[Result])

  class PathBuilder(path: List[String]):
    def as[A](using decoder: ConfigDecoder[A]): ConfigDecoder[A] =
      decoder.at(path)

  def path(path: String | List[String]) =
    path match
      case path: String       => PathBuilder(List(path))
      case path: List[String] => PathBuilder(path)

  def missingError[A]: Result[A] =
    NonEmptyChain(ConfigDecoderError.missing).raiseError[Result, A]

  def invalidError[A](expected: String): Result[A] =
    NonEmptyChain(ConfigDecoderError.invalid(expected)).raiseError[Result, A]

  given ConfigDecoder[String] =
    instance {
      case Config.Str(value) => value.pure[Result]
      case config            => invalidError("string")
    }

  given ConfigDecoder[Int] =
    instance {
      case Config.Num(value) => value.pure[Result]
      case config            => invalidError("int")
    }

  given monad: Monad[ConfigDecoder] with
    private val resultMonad = Monad[Result]

    override def pure[A](a: A): ConfigDecoder[A] =
      instance(_ => a.pure[Result])

    override def flatMap[A, B](fa: ConfigDecoder[A])(f: A => ConfigDecoder[B]): ConfigDecoder[B] =
      instance(config => fa.decode(config).flatMap(a => f(a).decode(config)))

    override def tailRecM[A, B](a: A)(f: A => ConfigDecoder[Either[A, B]]): ConfigDecoder[B] =
      instance(config => resultMonad.tailRecM(a)(f(_).decode(config)))

  object applicative extends Applicative[ConfigDecoder]:
    override def pure[A](a: A): ConfigDecoder[A] =
      instance(_ => a.pure[Result])

    override def ap[A, B](ff: ConfigDecoder[A => B])(fa: ConfigDecoder[A]): ConfigDecoder[B] =
      instance(config => ff.decode(config).toValidated.ap(fa.decode(config).toValidated).toEither)

  given parallel: Parallel[ConfigDecoder] with
    type F[A] = ConfigDecoder[A]

    override def applicative: Applicative[ConfigDecoder] =
      ConfigDecoder.applicative

    override def monad: Monad[ConfigDecoder] =
      ConfigDecoder.monad

    override def parallel: ConfigDecoder ~> ConfigDecoder =
      FunctionK.id

    override def sequential: ConfigDecoder ~> ConfigDecoder =
      FunctionK.id

end ConfigDecoder

extension [A](result: ConfigDecoder.Result[A])
  def prefix(path: String | List[String]): ConfigDecoder.Result[A] =
    result.left.map(_.map(_.at(path)))
