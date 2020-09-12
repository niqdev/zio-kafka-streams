package zio.kafka
package streams

import zio._
import zio.console._

abstract class KafkaStreamsApp(
  topologyLayer: RLayer[ZEnv, KafkaStreamsConfig with KafkaStreamsTopology]
) extends App {

  override def run(args: List[String]): URIO[ZEnv, ExitCode] =
    kafkaStreamsApp.provideLayer(kafkaStreamsLayer).exitCode

  private[this] final lazy val kafkaStreamsApp
    : RIO[Console with KafkaStreamsConfig with KafkaStreamsTopology, Unit] =
    for {
      config <- KafkaStreamsConfig.config
      _      <- putStr(config.prettyPrint)
      _      <- KafkaStreamsRuntime.make.useForever
    } yield ()

  private[this] final lazy val kafkaStreamsLayer =
    Console.live ++ topologyLayer
}
