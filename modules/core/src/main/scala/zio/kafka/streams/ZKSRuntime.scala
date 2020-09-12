package zio.kafka
package streams

import org.apache.kafka.streams.KafkaStreams
import zio._
import zio.console._

object ZKSRuntime {

  /**
    * Initialize Kafka Streams Runtime
    */
  def make: RManaged[Console with Settings with ZKSTopology, KafkaStreams] =
    ZManaged.make(setup)(stop)

  private[this] def setup: RIO[Console with Settings with ZKSTopology, KafkaStreams] =
    for {
      _            <- putStrLn("Build topology ...")
      topology     <- ZKSTopology.build
      _            <- putStrLn("Setup runtime ...")
      properties   <- Settings.settings.flatMap(_.toJavaProperties)
      kafkaStreams <- ZIO.effect(new KafkaStreams(topology, properties))
      _            <- putStrLn("Start runtime ...")
      _            <- startKafkaStreams(kafkaStreams)
    } yield kafkaStreams

  private[this] def startKafkaStreams(kafkaStreams: KafkaStreams): Task[KafkaStreams] =
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
  // TODO duration
  private[this] def stop: KafkaStreams => URIO[Console with Settings, Unit] =
    kafkaStreams =>
      (for {
        _        <- putStrLn("Stop runtime ...")
        settings <- Settings.settings
        _ <-
          Task
            .effect(kafkaStreams.close(java.time.Duration.ofSeconds(settings.shutdownTimeout)))
            .retryN(5)
      } yield ()).catchAll(_ => ZIO.unit)

}
