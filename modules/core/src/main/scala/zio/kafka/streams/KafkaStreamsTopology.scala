package zio.kafka.streams

import org.apache.kafka.streams.Topology
import zio._
import zio.config.ZConfig
import zio.logging.Logging

// TODO ZKSTopology
object KafkaStreamsTopology {
  type KafkaStreamsTopology = Has[KafkaStreamsTopology.Service]

  trait Service {
    def build: Task[Topology]
  }

  def make[T <: KafkaStreamsSettings: Tag](
    effect: ZIO[Logging with ZConfig[T], Throwable, Topology]
  ): ZLayer[Logging with ZConfig[T], Throwable, KafkaStreamsTopology] =
    effect
      .mapEffect(topology =>
        new Service {
          override def build: Task[Topology] =
            IO.succeed(topology)
        }
      )
      .toLayer

  def buildTopology: RIO[KafkaStreamsTopology, Topology] =
    ZIO.accessM[KafkaStreamsTopology](_.get.build)
}
