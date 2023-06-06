package config

import cats.arrow.FunctionK
import cats.data.Validated
import cats.{~>, Applicative, FlatMap, Monad, Parallel}
import cats.syntax.all.*

/** Type class for decoding instances of `Config` as domain-specific types.
  *
  * Synopsis:
  *
  *   - ConfigDecoder.identity - a `ConfigDecoder[Config]` that simply returns the whole config;
  *   - ConfigDecoder.field("foo") - a `ConfigDecoder[Config]` that returns the sub-config at path "foo";
  *   - ConfigDecoder.field("foo").field("bar") - a `ConfigDecoder[Config]` that returns the sub-config at path "foo.bar";
  *   - ConfigDecoder.field("foo").as[Bar] - a `ConfigDecoder[String]` that decodes the sub-config at path "foo" as a value of type `Bar`.
  *     Assumes a `using` parameter of type `ConfigDecoder[Bar]` is in implicit scope.
  *
  * Composition:
  *
  * `ConfigDecoder` is a monad allowing sequential composition of decoders. Semantics are fail-fast - if the first decoder fails, subsequent
  * decoders are not used and only one error message is returned:
  *
  * ```
  * val cellDecoder: ConfigDecoder[(String, Int)] =
  *   for
  *     col <- ConfigDecoder.field("col").as[String]
  *     row <- ConfigDecoder.field("row").as[Int]
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

  def map[B](func: A => B): ConfigDecoder[B] =
    ConfigDecoder.instance(config => decode(config).map(func))

  def eitherMap[B](func: A => Result[B]): ConfigDecoder[B] =
    ConfigDecoder.instance(config => decode(config).flatMap(func))

  def flatMap[B](func: A => ConfigDecoder[B]): ConfigDecoder[B] =
    ConfigDecoder.instance(config => decode(config).flatMap(a => func(a).decode(config)))

  def field(path: String): ConfigDecoder[Config] =
    ConfigDecoder.instance {
      case Config.Block(fields) =>
        fields.get(path).toRight(Vector(ConfigDecoderError.missing.at(path)))

      case Config.Str(value) =>
        Left(Vector(ConfigDecoderError.invalid("dict")))

      case Config.Num(value) =>
        Left(Vector(ConfigDecoderError.invalid("dict")))
    }

  def as[B](using dec: ConfigDecoder[B])(using ev: A =:= Config): ConfigDecoder[B] =
    ConfigDecoder.instance(config => this.decode(config).map(ev.apply).flatMap(dec.decode))

object ConfigDecoder:
  type Errors    = Vector[ConfigDecoderError]
  type Result[A] = Either[Errors, A]

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

  def field(path: String): ConfigDecoder[Config] =
    identity.field(path)

  given ConfigDecoder[String] =
    instance {
      case Config.Str(value) => value.pure[Result]
      case _                 => Vector(ConfigDecoderError.invalid("string")).raiseError
    }

  given ConfigDecoder[Int] =
    instance {
      case Config.Num(value) => value.pure[Result]
      case _                 => Vector(ConfigDecoderError.invalid("int")).raiseError
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
