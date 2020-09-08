package zio.kafka.streams

import kafka.streams.serde.RecordConsumed
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Printed
import org.apache.kafka.streams.scala.StreamsBuilder
import zio._

// TODO refined dependency or not?
sealed abstract class ZStreamsBuilder(private val builder: StreamsBuilder) {

  def stream[K, V](topic: String)(
    implicit C: RecordConsumed[K, V]
  ): Task[ZKStream[K, V]] =
    ZKStream(builder.stream(topic)(C.consumed))

  // TODO debug enable in ZEnv?
  def streamWithLog[K, V](topic: String)(
    implicit C: RecordConsumed[K, V]
  ): Task[ZKStream[K, V]] =
    ZKStream {
      val stream = builder.stream(topic)(C.consumed)
      stream.print(Printed.toSysOut[K, V].withLabel(topic))
      stream
    }

  def stream[K, V](topics: Set[String])(
    implicit C: RecordConsumed[K, V]
  ): Task[ZKStream[K, V]] =
    ZKStream(builder.stream(topics)(C.consumed))
}

object ZStreamsBuilder {

  def newInstance: Task[ZStreamsBuilder] =
    Task.effect(new ZStreamsBuilder(new StreamsBuilder()) {})

  // TODO RIO[Logging with ZConfig[T], Topology]
  def apply(f: ZStreamsBuilder => Task[Unit]): Task[Topology] =
    newInstance.flatMap(zsb => f(zsb) *> Task.effect(zsb.builder.build()))
}
