package zio.kafka.streams

import org.apache.kafka.streams.KafkaStreams
import zio._
import zio.config.{ ZConfig, config }
import zio.kafka.streams.KafkaStreamsTopology._
import zio.logging.{ Logging, log }

// TODO ZKSRuntime
object KafkaStreamsRuntime {
  final type KafkaStreamRuntimeEnv[T <: KafkaStreamsSettings] = Logging
    with ZConfig[T]
    with KafkaStreamsTopology

  /**
    * TODO docs
    */
  def make[T <: KafkaStreamsSettings: Tag]: ZManaged[KafkaStreamRuntimeEnv[T], Throwable, KafkaStreams] =
    ZManaged.make(setup[T])(stop)

  private[this] def setup[T <: KafkaStreamsSettings: Tag]
    : ZIO[KafkaStreamRuntimeEnv[T], Throwable, KafkaStreams] =
    for {
      settings     <- config[T]
      _            <- log.info("Build topology ...")
      topology     <- buildTopology
      _            <- log.info("Setup runtime ...")
      kafkaStreams <- ZIO.effect(new KafkaStreams(topology, settings.properties))
      _            <- log.info("Start runtime ...")
      _            <- startKafkaStreams[T](kafkaStreams)
    } yield kafkaStreams

  private[this] def startKafkaStreams[T <: KafkaStreamsSettings: Tag](
    kafkaStreams: KafkaStreams
  ): ZIO[KafkaStreamRuntimeEnv[T], Throwable, KafkaStreams] =
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
