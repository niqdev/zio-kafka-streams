package zio.kafka
package streams

import org.apache.kafka.streams.Topology
import zio._

object KafkaStreamsTopology {
  type KafkaStreamsTopology = Has[KafkaStreamsTopology.Service]

  trait Service {

    /**
      * Build Kafka Streams topology
      */
    def build: Task[Topology]
  }

  def make[T](effect: RIO[T, Topology]): RLayer[T, KafkaStreamsTopology] =
    effect
      .mapEffect(topology =>
        new Service {
          override def build: Task[Topology] =
            Task.succeed(topology)
        }
      )
      .toLayer

  def build: RIO[KafkaStreamsTopology, Topology] =
    ZIO.accessM[KafkaStreamsTopology](_.get.build)
}
