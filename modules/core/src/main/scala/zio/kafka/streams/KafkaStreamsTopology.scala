package zio.kafka.streams

import org.apache.kafka.streams.Topology
import zio._

object KafkaStreamsTopology {
  type KafkaStreamsTopology = Has[KafkaStreamsTopology.Service]

  trait Service {
    def build: Task[Topology]
  }

  def build: RIO[KafkaStreamsTopology, Topology] =
    ZIO.accessM[KafkaStreamsTopology](_.get.build)
}
