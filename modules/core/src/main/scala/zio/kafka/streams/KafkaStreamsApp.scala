package zio.kafka.streams

import zio._
import zio.config.{ ReadError, ZConfig, config, write }
import zio.kafka.streams.settings.Settings
import zio.logging.{ Logging, log }

abstract class KafkaStreamsApp[T: Tag](
  kafkaStreamsLayer: ZLayer[ZConfig[Settings], ReadError[String], KafkaStreamsEnv[T]]
) extends App {

  private[this] final val program =
    for {
      settings       <- config[Settings]
      customSettings <- config[T] // TODO print Json
      _              <- log.info(s"${write(Settings.descriptor, settings).map(_.flattenString())}")
      _              <- KafkaStreamsRuntime.make.useForever
    } yield ()

  override def run(args: List[String]): URIO[ZEnv, ExitCode] =
    program.provideLayer(Logging.console() ++ Settings.configEnvLayer >+> kafkaStreamsLayer).exitCode
}
