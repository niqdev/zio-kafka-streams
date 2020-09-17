package com.github.niqdev

import cats.effect.{ ExitCode, IO, IOApp, Resource, Sync }
import cats.syntax.flatMap._
import cats.syntax.functor._
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig
import kafka.streams.serde._
import org.apache.kafka.streams.kstream.Printed
import org.apache.kafka.streams.scala.StreamsBuilder
import org.apache.kafka.streams.scala.kstream.KStream
import org.apache.kafka.streams.{ KafkaStreams, StreamsConfig, Topology }

object syntax {
  final implicit def streamsBuilderSyntax[F[_]](builder: StreamsBuilder): StreamsBuilderOps[F] =
    new StreamsBuilderOps(builder)

  final implicit def kStreamSyntax[F[_], K, V](kStream: KStream[K, V]): KStreamOps[F, K, V] =
    new KStreamOps(kStream)
}

final class StreamsBuilderOps[F[_]](private val builder: StreamsBuilder) extends AnyVal {

  def streamF[K, V](
    topic: String,
    schemaRegistryUrl: String
  )(implicit F: Sync[F], C: AvroRecordConsumed[K, V]): F[KStream[K, V]] =
    F.delay {
      val kstream = builder.stream(topic)(C.consumed(schemaRegistryUrl))
      kstream.print(Printed.toSysOut[K, V].withLabel(topic))
      kstream
    }
}

final class KStreamOps[F[_], K, V](private val kStream: KStream[K, V]) extends AnyVal {

  def toF(
    topic: String,
    schemaRegistryUrl: String
  )(implicit F: Sync[F], P: AvroRecordProduced[K, V]): F[Unit] =
    F.delay {
      kStream.print(Printed.toSysOut[K, V].withLabel(topic))
      kStream.to(topic)(P.produced(schemaRegistryUrl))
    }
}

sealed abstract class CatsAvroTopology[F[_]](implicit F: Sync[F]) {
  import com.github.niqdev.schema.dummy._
  import com.github.niqdev.syntax._

  // IN -> OUT
  def buildTopology: F[Topology] =
    for {
      schemaRegistryUrl <- F.pure("http://localhost:8081")
      builder           <- F.delay(new StreamsBuilder())
      source            <- builder.streamF[DummyKey, DummyValue]("source-topic", schemaRegistryUrl)
      _                 <- source.toF("sink-topic", schemaRegistryUrl)
      topology          <- F.delay(builder.build())
    } yield topology
}
object CatsAvroTopology {
  def apply[F[_]: Sync]: CatsAvroTopology[F] =
    new CatsAvroTopology[F] {}
}

// sbt "examples/runMain com.github.niqdev.KafkaStreamsCatsApp"
object KafkaStreamsCatsApp extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    runApp[IO].as(ExitCode.Success)

  // use Async to keep the app running forever
  // see also https://github.com/niqdev/kafka-scala-examples/tree/master/cats-kafka-streams/src/main/scala/com/kafka/demo
  private[this] def runApp[F[_]: Sync]: F[Unit] =
    Resource
      .make[F, KafkaStreams](setup)(stop)
      .use(kafkaStreams => Sync[F].delay(kafkaStreams.start()))

  private[this] def setup[F[_]: Sync]: F[KafkaStreams] =
    for {
      properties <- Sync[F].delay {
        val props = new java.util.Properties
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "kafka-streams-cats")
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
        props.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081")
        props
      }
      topology <- CatsAvroTopology[F].buildTopology
      streams  <- Sync[F].delay(new KafkaStreams(topology, properties))
    } yield streams

  private[this] def stop[F[_]: Sync]: KafkaStreams => F[Unit] =
    kafkaStreams => Sync[F].delay(kafkaStreams.close(java.time.Duration.ofSeconds(1)))
}
