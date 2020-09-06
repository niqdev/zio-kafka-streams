package zio.kafka.streams

import org.apache.kafka.streams.KafkaStreams
import zio._
import zio.config.{ ZConfig, config }
import zio.kafka.streams.KafkaStreamsTopology.KafkaStreamsTopology
import zio.logging.{ Logging, log }

object KafkaStreamsRuntime {
  final type KafkaStreamRuntimeEnv[T <: KafkaStreamsSettings] = Logging
    with ZConfig[T]
    with KafkaStreamsTopology

  // TODO add errorHandler
  private[this] def start[T <: KafkaStreamsSettings: Tag]
    : ZIO[KafkaStreamRuntimeEnv[T], Throwable, KafkaStreams] =
    for {
      settings     <- config[T]
      _            <- log.info("Build topology ...")
      topology     <- KafkaStreamsTopology.build
      _            <- log.info("Setup runtime ...")
      kafkaStreams <- ZIO.effect(new KafkaStreams(topology, settings.properties))
      _            <- log.info("Start runtime ...")
      _            <- ZIO.effect(kafkaStreams.start())
    } yield kafkaStreams

  // TODO retry
  // TODO catchAll ??? release accepts URIO i.e. convert Throwable to Nothing
  // effectTotal ??? https://github.com/zio/zio-kafka/blob/master/src/main/scala/zio/kafka/admin/AdminClient.scala#L206
  private[this] def stop: KafkaStreams => URIO[Logging, Unit] =
    kafkaStreams =>
      for {
        _ <- log.info("Stop runtime ...")
        _ <- ZIO.effectTotal(kafkaStreams.close(java.time.Duration.ofSeconds(1)))
      } yield ()

  def make[T <: KafkaStreamsSettings: Tag]: ZManaged[KafkaStreamRuntimeEnv[T], Throwable, KafkaStreams] =
    ZManaged.make(start[T])(stop)
}
