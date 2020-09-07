package zio.kafka
package streams

import org.apache.kafka.streams.KafkaStreams
import zio._
import zio.config._
import zio.logging._

// TODO KafkaStreamsSettings
object ZKSRuntime {
  final type ZKSRuntime[T <: KafkaStreamsSettings] = Logging with ZConfig[T] with ZKSTopology

  /**
    * TODO docs
    */
  def make[T <: KafkaStreamsSettings: Tag]: ZManaged[ZKSRuntime[T], Throwable, KafkaStreams] =
    ZManaged.make(setup[T])(stop)

  private[this] def setup[T <: KafkaStreamsSettings: Tag]: ZIO[ZKSRuntime[T], Throwable, KafkaStreams] =
    for {
      settings     <- ZIO.access[ZConfig[T]](_.get)
      _            <- log.info("Build topology ...")
      topology     <- ZKSTopology.buildTopology
      _            <- log.info("Setup runtime ...")
      kafkaStreams <- ZIO.effect(new KafkaStreams(topology, settings.properties))
      _            <- log.info("Start runtime ...")
      _            <- startKafkaStreams[T](kafkaStreams)
    } yield kafkaStreams

  private[this] def startKafkaStreams[T <: KafkaStreamsSettings: Tag](
    kafkaStreams: KafkaStreams
  ): ZIO[ZKSRuntime[T], Throwable, KafkaStreams] =
    ZIO.effectAsyncM { callback =>

      def setupShutdownHandler =
        ZIO.effect {
          kafkaStreams
            .setUncaughtExceptionHandler((_: Thread, throwable: Throwable) => callback(IO.fail(throwable)))

          kafkaStreams
            .setStateListener((newState: KafkaStreams.State, _: KafkaStreams.State) =>
              newState match {
                case KafkaStreams.State.ERROR =>
                  callback(IO.fail(new IllegalStateException("Shut down application in ERROR state")))
                case KafkaStreams.State.NOT_RUNNING =>
                  // TODO is this ok?
                  callback(IO.succeed(kafkaStreams))
                case _ => ()
              }
            )
        }

      // to gracefully shutdown in response to SIGTERM
      def setupGracefulShutdown =
        ZIO.effect {
          java.lang.Runtime.getRuntime.addShutdownHook(new Thread(() => kafkaStreams.close()))
        }

      for {
        _ <- setupShutdownHandler
        _ <- ZIO.effect(kafkaStreams.start())
        _ <- setupGracefulShutdown
      } yield ()
    }

  // TODO retryN + repeat(Schedule)
  // effectTotal ??? https://github.com/zio/zio-kafka/blob/master/src/main/scala/zio/kafka/admin/AdminClient.scala#L206
  private[this] def stop: KafkaStreams => URIO[Logging, Unit] =
    kafkaStreams =>
      for {
        _ <- log.info("Stop runtime ...")
        _ <-
          ZIO
            .effect(kafkaStreams.close(java.time.Duration.ofSeconds(2)))
            .retryN(5)
            .catchAll(_ => ZIO.unit)
      } yield ()
}
