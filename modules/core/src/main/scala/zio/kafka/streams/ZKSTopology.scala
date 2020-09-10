package zio.kafka
package streams

import org.apache.kafka.streams.Topology
import zio._

object ZKSTopology {
  type ZKSTopology = Has[ZKSTopology.Service]

  trait Service {

    /**
      * Build Kafka Streams topology
      */
    def build: Task[Topology]
  }

  def make(effect: RIO[TopologyEnv, Topology]): RLayer[TopologyEnv, ZKSTopology] =
    effect
      .mapEffect(topology =>
        new Service {
          override def build: Task[Topology] =
            IO.succeed(topology)
        }
      )
      .toLayer

  def build: RIO[ZKSTopology, Topology] =
    ZIO.accessM[ZKSTopology](_.get.build)
}
