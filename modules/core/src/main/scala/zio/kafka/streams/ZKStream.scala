package zio.kafka
package streams

import kafka.streams.serde._
import org.apache.kafka.streams.kstream.Printed
import org.apache.kafka.streams.scala.kstream._
import zio._

sealed abstract class ZKStream[K, V](private val stream: KStream[K, V]) {

  def mapValues[VO](f: V => VO): Task[ZKStream[K, VO]] =
    ZKStream(stream.mapValues(f))

  def toProduced(topic: String): Produced[K, V] => RIO[KafkaStreamsConfig, Unit] =
    produced =>
      for {
        config <- KafkaStreamsConfig.config
        _ <- Task.effect {
          if (config.debug) stream.print(Printed.toSysOut[K, V].withLabel(topic))
          stream.to(topic)(produced)
        }
      } yield ()

  def to(topic: String)(
    implicit P: RecordProduced[K, V]
  ): RIO[KafkaStreamsConfig, Unit] =
    toProduced(topic)(P.produced)

  // TODO schemaRegistryUrl.get
  def toAvro(topic: String)(
    implicit P: AvroRecordProduced[K, V]
  ): RIO[KafkaStreamsConfig, Unit] =
    KafkaStreamsConfig
      .config
      .flatMap(settings => toProduced(topic)(P.produced(settings.schemaRegistryUrl.get)))
}

object ZKStream {

  def apply[K, V](stream: KStream[K, V]): Task[ZKStream[K, V]] =
    Task.effect(new ZKStream[K, V](stream) {})
}
