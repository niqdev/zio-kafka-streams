package zio.kafka
package streams

import kafka.streams.serde._
import org.apache.kafka.streams.scala.kstream.KStream
import zio._

sealed abstract class ZKStream[K, V](private val kstream: KStream[K, V]) {

  def mapValues[VO](f: V => VO): Task[ZKStream[K, VO]] =
    ZKStream(kstream.mapValues(f))

  def to(topic: String)(
    implicit P: RecordProduced[K, V]
  ): Task[Unit] =
    Task.effect(kstream.to(topic)(P.produced))
}

object ZKStream {

  def apply[K, V](stream: KStream[K, V]): Task[ZKStream[K, V]] =
    Task.effect(new ZKStream[K, V](stream) {})
}
