package zio.kafka.streams

import org.apache.kafka.streams.Topology
import zio._
import zio.logging.Logger

object KafkaStreamsTopology {
  type KafkaStreamsTopology = Has[KafkaStreamsTopology.Service]

  trait Service {
    def build: Task[Topology]
  }

  def make[T <: KafkaStreamsSettings: Tag](buildTopology: (Logger[String], T) => Task[Topology]) =
    ZLayer.fromServices((log: Logger[String], settings: T) =>
      new Service {
        override def build: Task[Topology] =
          buildTopology(log, settings)
      }
    )

  def build: RIO[KafkaStreamsTopology, Topology] =
    ZIO.accessM[KafkaStreamsTopology](_.get.build)
}
