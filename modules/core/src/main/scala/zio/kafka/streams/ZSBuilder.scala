package zio.kafka.streams

import kafka.streams.serde.RecordConsumed
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.scala.StreamsBuilder
import zio._

object ZSBuilder {

  def stream[K, V](builder: StreamsBuilder, topic: String)(
    implicit C: RecordConsumed[K, V]
  ): Task[ZKStream[K, V]] =
    ZKStream(builder.stream(topic)(C.consumed))

  def apply(f: StreamsBuilder => Task[Unit]): Task[Topology] =
    Task.effect(new StreamsBuilder()).flatMap(builder => f(builder) *> Task.effect(builder.build()))
}
