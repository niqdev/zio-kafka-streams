package zio.kafka
package streams

import zio._
import zio.console._

abstract class KafkaStreamsApp(
  topologyLayer: ZLayer[ZEnv, Throwable, Settings with ZKSTopology]
) extends App {

  override def run(args: List[String]): URIO[ZEnv, ExitCode] =
    kafkaStreamsApp.provideLayer(kafkaStreamsLayer).exitCode

  private[this] final lazy val kafkaStreamsApp: RIO[Console with Settings with ZKSTopology, Unit] =
    for {
      settings <- Settings.settings
      _        <- putStr(settings.prettyPrint)
      _        <- ZKSRuntime.make.useForever
    } yield ()

  private[this] final lazy val kafkaStreamsLayer =
    Console.live ++ topologyLayer
}
