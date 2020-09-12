package zio.kafka
package streams

import kafka.streams.serde._
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Printed
import org.apache.kafka.streams.scala.StreamsBuilder
import org.apache.kafka.streams.scala.kstream._
import zio._

// TODO Refined ?
sealed abstract class ZStreamsBuilder(private val builder: StreamsBuilder) {

  def streamConsumed[K, V](topic: String): Consumed[K, V] => RIO[Settings, ZKStream[K, V]] =
    consumed =>
      for {
        settings <- Settings.settings
        stream <- ZKStream {
          val stream = builder.stream(topic)(consumed)
          if (settings.debug) stream.print(Printed.toSysOut[K, V].withLabel(topic))
          stream
        }
      } yield stream

  def stream[K, V](topic: String)(
    implicit C: RecordConsumed[K, V]
  ): RIO[Settings, ZKStream[K, V]] =
    streamConsumed(topic)(C.consumed)

  // TODO schemaRegistryUrl.get
  def streamAvro[K, V](topic: String)(
    implicit C: AvroRecordConsumed[K, V]
  ): RIO[Settings, ZKStream[K, V]] =
    Settings.settings.flatMap(settings => streamConsumed(topic)(C.consumed(settings.schemaRegistryUrl.get)))
}

object ZStreamsBuilder {

  def newInstance: Task[ZStreamsBuilder] =
    Task.effect(new ZStreamsBuilder(new StreamsBuilder()) {})

  // TODO
  def apply(f: ZStreamsBuilder => RIO[Settings, Unit]): RIO[Settings, Topology] =
    newInstance.flatMap(zsb => f(zsb) *> Task.effect(zsb.builder.build()))
}
