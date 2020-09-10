package zio.kafka
package streams

import org.apache.kafka.streams.Topology
import zio._
import zio.logging._

abstract class KafkaStreamsApp(settingsLayer: ULayer[Settings]) extends App {

  override def run(args: List[String]): URIO[ZEnv, ExitCode] =
    kafkaStreamsApp.provideLayer(kafkaStreamsLayer).exitCode

  private[this] final lazy val kafkaStreamsApp: RIO[KafkaStreamsEnv, Unit] =
    for {
      settings <- Settings.config
      _        <- log.info(s"AppSettings: $settings")
      _        <- ZKSRuntime.make.useForever
    } yield ()

  private[this] final lazy val kafkaStreamsLayer =
    Logging.console() ++ settingsLayer >+> ZKSTopology.make(runApp)

  /**
    * Run Kafka Streams application
    */
  def runApp: RIO[TopologyEnv, Topology]
}
