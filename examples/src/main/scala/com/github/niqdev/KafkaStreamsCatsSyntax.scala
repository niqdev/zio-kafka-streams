package com.github.niqdev

import cats.effect.Sync
import kafka.streams.serde._
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Printed
import org.apache.kafka.streams.scala.StreamsBuilder
import org.apache.kafka.streams.scala.kstream.KStream

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

object dummy {
  final case class DummyKey(uuid: java.util.UUID)
  object DummyKey {
    final implicit val dummyKeyAvroCodec: AvroCodec[DummyKey] =
      AvroCodec.genericKey[DummyKey]
  }
  final case class DummyValue(value: String)
  object DummyValue {
    final implicit val dummyValueAvroCodec: AvroCodec[DummyValue] =
      AvroCodec.genericValue[DummyValue]
  }
}

sealed abstract class CatsTopologyExample[F[_]](implicit F: Sync[F]) {
  import cats.syntax.flatMap._
  import cats.syntax.functor._
  import dummy._
  import syntax._

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
object CatsTopologyExample {
  def apply[F[_]: Sync]: CatsTopologyExample[F] =
    new CatsTopologyExample[F] {}
}
