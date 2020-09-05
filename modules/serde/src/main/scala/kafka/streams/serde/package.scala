package kafka.streams

import kafka.streams.serde.internal.EitherOps

package object serde {

  private[serde] final implicit def eitherSyntax(either: Either.type): EitherOps =
    new EitherOps(either)
}
