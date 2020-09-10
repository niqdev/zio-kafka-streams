package zio.kafka
package streams

import kafka.streams.serde._
import org.apache.kafka.streams.kstream.Printed
import org.apache.kafka.streams.scala.kstream._
import zio._

sealed abstract class ZKStream[K, V](private val stream: KStream[K, V]) {

  def mapValues[VO](f: V => VO): Task[ZKStream[K, VO]] =
    ZKStream(stream.mapValues(f))

  def toProduced(topic: String): Produced[K, V] => RIO[Settings, Unit] =
    produced =>
      for {
        settings <- Settings.config
        _ <- Task.effect {
          if (settings.debug) stream.print(Printed.toSysOut[K, V].withLabel(topic))
          stream.to(topic)(produced)
        }
      } yield stream

  def to(topic: String)(
    implicit P: RecordProduced[K, V]
  ): RIO[Settings, Unit] =
    toProduced(topic)(P.produced)

  def toAvro(topic: String)(
    implicit P: AvroRecordProduced[K, V]
  ): RIO[Settings, Unit] =
    Settings.config.flatMap(settings => toProduced(topic)(P.produced(settings.schemaRegistryUrl)))
}

object ZKStream {

  def apply[K, V](stream: KStream[K, V]): Task[ZKStream[K, V]] =
    Task.effect(new ZKStream[K, V](stream) {})
}
