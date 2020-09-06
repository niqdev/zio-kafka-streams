package zio.kafka.streams

import org.apache.kafka.streams.Topology
import zio._
import zio.config.magnolia.DeriveConfigDescriptor._
import zio.config.{ ZConfig, config, write }
import zio.kafka.streams.settings.AppSettings
import zio.logging.{ Logger, Logging, log }

abstract class KafkaStreamsApp[T: Tag] extends App {

  private[this] final lazy val program: ZIO[KafkaStreamsEnv[T], Throwable, Unit] =
    for {
      appSettings      <- config[AppSettings]
      topologySettings <- config[T]
      _                <- log.info(s"${write(AppSettings.descriptor, appSettings).map(_.flattenString())}")
      _                <- log.info(s"${write(descriptor[T], topologySettings).map(_.flattenString())}")
      _                <- KafkaStreamsRuntime.make.useForever
    } yield ()

  // TODO allow to override env
  private[this] final lazy val kafkaStreamsLayer =
    Logging.console() ++ AppSettings.configEnvLayer ++ ZConfig.fromSystemEnv(
      descriptor[T]
    ) >+> KafkaStreamsTopology.make[T](topology)

  override def run(args: List[String]): URIO[ZEnv, ExitCode] =
    program.provideLayer(kafkaStreamsLayer).exitCode

  def topology(log: Logger[String], appSettings: AppSettings, topologySettings: T): Task[Topology]
}
