package zio.kafka
package streams

import kafka.streams.serde._
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Printed
import org.apache.kafka.streams.scala.StreamsBuilder
import org.apache.kafka.streams.scala.kstream._
import zio._

sealed abstract class ZStreamsBuilder(private val builder: StreamsBuilder) {

  def streamConsumed[K, V](topic: String): Consumed[K, V] => RIO[KafkaStreamsConfig, ZKStream[K, V]] =
    consumed =>
      for {
        config <- KafkaStreamsConfig.config
        stream <- ZKStream {
          val stream = builder.stream(topic)(consumed)
          if (config.debug) stream.print(Printed.toSysOut[K, V].withLabel(topic))
          stream
        }
      } yield stream

  def stream[K, V](topic: String)(
    implicit C: RecordConsumed[K, V]
  ): RIO[KafkaStreamsConfig, ZKStream[K, V]] =
    streamConsumed(topic)(C.consumed)

  def streamAvro[K, V](topic: String)(
    implicit C: AvroRecordConsumed[K, V]
  ): RIO[KafkaStreamsConfig, ZKStream[K, V]] =
    KafkaStreamsConfig
      .requiredSchemaRegistryUrl
      .flatMap(schemaRegistryUrl => streamConsumed(topic)(C.consumed(schemaRegistryUrl)))
}

object ZStreamsBuilder {

  def newInstance: Task[ZStreamsBuilder] =
    Task.effect(new ZStreamsBuilder(new StreamsBuilder()) {})

  def apply(f: ZStreamsBuilder => RIO[KafkaStreamsConfig, Unit]): RIO[KafkaStreamsConfig, Topology] =
    newInstance.flatMap(zsb => f(zsb) *> Task.effect(zsb.builder.build()))
}
