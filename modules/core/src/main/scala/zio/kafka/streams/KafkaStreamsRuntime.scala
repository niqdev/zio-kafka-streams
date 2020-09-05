package zio.kafka.streams

import org.apache.kafka.streams.KafkaStreams
import zio._
import zio.config.{ ZConfig, config }
import zio.kafka.streams.KafkaStreamsTopology.KafkaStreamsTopology
import zio.kafka.streams.settings.Settings
import zio.logging.{ Logging, log }

object KafkaStreamsRuntime {

  private[this] def start
    : ZIO[Logging with ZConfig[Settings] with KafkaStreamsTopology, Throwable, KafkaStreams] =
    for {
      _            <- log.info("Setup runtime ...")
      settings     <- config[Settings]
      topology     <- KafkaStreamsTopology.build
      kafkaStreams <- ZIO.effect(new KafkaStreams(topology, settings.properties()))
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

  def make: ZManaged[Logging with ZConfig[Settings] with KafkaStreamsTopology, Throwable, KafkaStreams] =
    ZManaged.make(start)(stop)
}
