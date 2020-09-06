package zio.kafka.streams

import org.apache.kafka.streams.Topology
import zio._
import zio.config.{ ReadError, ZConfig, config }
import zio.logging.{ Logger, Logging, log }

abstract class KafkaStreamsApp[T <: KafkaStreamsSettings: Tag](
  configLayer: Layer[ReadError[String], ZConfig[T]]
) extends App {

  override def run(args: List[String]): URIO[ZEnv, ExitCode] =
    program.provideLayer(kafkaStreamsLayer).exitCode

  private[this] final lazy val program: ZIO[KafkaStreamsEnv[T], Throwable, Unit] =
    for {
      settings <- config[T]
      //_ <- log.info(s"${write(descriptor[T], settings).map(_.flattenString())}")
      _ <- log.info(s"KafkaStreamsApp ${settings.applicationId}")
      _ <- KafkaStreamsRuntime.make.useForever
    } yield ()

  private[this] final lazy val kafkaStreamsLayer =
    Logging.console() ++ configLayer >+> KafkaStreamsTopology.make[T](topology)

  /**
    * TODO docs
    */
  def topology(log: Logger[String], settings: T): Task[Topology]
}
