package zio.kafka
package streams

import org.apache.kafka.streams.KafkaStreams
import zio._
import zio.logging._

object ZKSRuntime {

  /**
    * Initialize Kafka Streams Runtime
    */
  def make: RManaged[KafkaStreamsEnv, KafkaStreams] =
    ZManaged.make(setup)(stop)

  private[this] def setup: RIO[KafkaStreamsEnv, KafkaStreams] =
    for {
      settings     <- Settings.config
      _            <- log.info("Build topology ...")
      topology     <- ZKSTopology.build
      _            <- log.info("Setup runtime ...")
      kafkaStreams <- ZIO.effect(new KafkaStreams(topology, settings.properties))
      _            <- log.info("Start runtime ...")
      _            <- startKafkaStreams(kafkaStreams)
    } yield kafkaStreams

  private[this] def startKafkaStreams(kafkaStreams: KafkaStreams): RIO[KafkaStreamsEnv, KafkaStreams] =
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

  // TODO retryN + repeat(Schedule) configurable in Settings
  private[this] def stop: KafkaStreams => URIO[Logging, Unit] =
    kafkaStreams =>
      log.info("Stop runtime ...") *> ZIO
        .effect(kafkaStreams.close(java.time.Duration.ofSeconds(2)))
        .retryN(5)
        .catchAll(_ => ZIO.unit)
        .flatMap(_ => ZIO.unit)

}
