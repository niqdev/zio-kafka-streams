package zio.kafka.streams

import org.apache.kafka.streams.Topology
import zio._
import zio.kafka.streams.settings.AppSettings
import zio.logging.Logger

object KafkaStreamsTopology {
  type KafkaStreamsTopology = Has[KafkaStreamsTopology.Service]

  trait Service {
    def build: Task[Topology]
  }

  def make[T: Tag](buildTopology: (Logger[String], AppSettings, T) => Task[Topology]) =
    ZLayer.fromServices((log: Logger[String], appSettings: AppSettings, topologySettings: T) =>
      new Service {
        override def build: Task[Topology] =
          buildTopology(log, appSettings, topologySettings)
      }
    )

  def build: RIO[KafkaStreamsTopology, Topology] =
    ZIO.accessM[KafkaStreamsTopology](_.get.build)
}
