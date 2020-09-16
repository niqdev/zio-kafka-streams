package zio.kafka
package streams

import org.apache.kafka.streams.KafkaStreams
import zio._
import zio.console._

object KafkaStreamsRuntime {

  /**
    * Initialize Kafka Streams runtime
    */
  def make: RManaged[Console with KafkaStreamsConfig with KafkaStreamsTopology, KafkaStreams] =
    ZManaged.make(setup)(stop)

  private[this] def setup: RIO[Console with KafkaStreamsConfig with KafkaStreamsTopology, KafkaStreams] =
    for {
      _            <- putStrLn("Build topology ...")
      topology     <- KafkaStreamsTopology.build
      _            <- putStrLn("Setup runtime ...")
      properties   <- KafkaStreamsConfig.config.flatMap(_.toJavaProperties)
      kafkaStreams <- Task.effect(new KafkaStreams(topology, properties))
      _            <- putStrLn("Start runtime ...")
      _            <- startKafkaStreams(kafkaStreams)
    } yield kafkaStreams

  private[this] def startKafkaStreams(kafkaStreams: KafkaStreams): Task[KafkaStreams] =
    Task.effectAsyncM { callback =>

      def setupShutdownHandler: Task[Unit] =
        Task.effect {
          kafkaStreams
            .setUncaughtExceptionHandler((_: Thread, throwable: Throwable) => callback(Task.fail(throwable)))

          kafkaStreams
            .setStateListener((newState: KafkaStreams.State, _: KafkaStreams.State) =>
              newState match {
                case KafkaStreams.State.ERROR =>
                  callback(Task.fail(new IllegalStateException("Shut down application in ERROR state")))
                case KafkaStreams.State.NOT_RUNNING =>
                  callback(Task.succeed(kafkaStreams))
                case _ => ()
              }
            )
        }

      // to gracefully shutdown in response to SIGTERM
      def setupGracefulShutdown: Task[Unit] =
        Task.effect(java.lang.Runtime.getRuntime.addShutdownHook(new Thread(() => kafkaStreams.close())))

      setupShutdownHandler *> Task.effect(kafkaStreams.start()) <* setupGracefulShutdown
    }

  // TODO retryN + repeat(Schedule) configurable in AppConfig
  private[this] def stop: KafkaStreams => URIO[Console with KafkaStreamsConfig, Unit] =
    kafkaStreams =>
      putStrLn("Stop runtime ...") *>
        KafkaStreamsConfig
          .config
          .flatMap(config =>
            Task
              .effect(kafkaStreams.close(java.time.Duration.ofSeconds(config.shutdownTimeout)))
              .retryN(5)
          )
          .ignore
}
