package zio.kafka
package streams

import org.apache.kafka.streams.Topology
import zio._
import zio.config._
import zio.logging._

// TODO remove constraint T <: KafkaStreamsSettings
abstract class KafkaStreamsApp[T <: KafkaStreamsSettings: Tag](
  configLayer: Layer[ReadError[String], ZConfig[T]]
) extends App {

  override def run(args: List[String]): URIO[ZEnv, ExitCode] =
    kafkaStreamsApp.provideLayer(kafkaStreamsLayer).exitCode

  // TODO print all settings
  private[this] final lazy val kafkaStreamsApp: RIO[Logging with ZConfig[T] with ZKSTopology, Unit] =
    for {
      settings <- ZIO.access[ZConfig[T]](_.get)
      //_ <- log.info(s"${write(descriptor[T], settings).map(_.flattenString())}")
      _ <- log.info(s"KafkaStreamsApp ${settings.applicationId}")
      _ <- ZKSRuntime.make[T].useForever
    } yield ()

  private[this] final lazy val kafkaStreamsLayer =
    Logging.console() ++ configLayer >+> ZKSTopology.make[T](runApp)

  /**
    * TODO docs
    */
  def runApp: RIO[Logging with ZConfig[T], Topology]
}
