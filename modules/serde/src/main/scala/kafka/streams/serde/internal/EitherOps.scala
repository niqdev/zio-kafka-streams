package kafka.streams.serde
package internal

private[serde] final class EitherOps(private val either: Either.type) extends AnyVal {

  // see cats.syntax.either.catsSyntaxEitherObject
  def catchNonFatal[A](f: => A): Either[Throwable, A] =
    try Right(f)
    catch {
      case scala.util.control.NonFatal(t) => Left(t)
    }
}
