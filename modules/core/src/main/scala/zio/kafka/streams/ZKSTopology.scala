package zio.kafka
package streams

import org.apache.kafka.streams.Topology
import zio._
import zio.config._
import zio.logging._

object ZKSTopology {
  type ZKSTopology = Has[ZKSTopology.Service]

  trait Service {
    def build: Task[Topology]
  }

  def make[T <: KafkaStreamsSettings: Tag](
    effect: ZIO[Logging with ZConfig[T], Throwable, Topology]
  ): ZLayer[Logging with ZConfig[T], Throwable, ZKSTopology] =
    effect
      .mapEffect(topology =>
        new Service {
          override def build: Task[Topology] =
            IO.succeed(topology)
        }
      )
      .toLayer

  def buildTopology: RIO[ZKSTopology, Topology] =
    ZIO.accessM[ZKSTopology](_.get.build)
}
