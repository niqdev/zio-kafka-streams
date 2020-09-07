package zio.kafka
package streams

import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.scala.StreamsBuilder
import zio.logging.Logging
import zio.{ RIO, ZIO }

object ZKStream {

  // TODO RIO[Logging with T, StreamsBuilder] => Task[Unit]
  def builder[T](f: StreamsBuilder => Unit): RIO[Logging with T, Topology] =
    ZIO.effect {
      val builder = new StreamsBuilder()
      f(builder)
      builder.build()
    }
}
