package com.github.niqdev

import cats.effect.Sync
import com.sksamuel.avro4s.{ Decoder, Encoder, SchemaFor }
import kafka.streams.serde._
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
    schemaRegistry: String
  )(implicit F: Sync[F], C: AvroRecordConsumed[K, V]): F[KStream[K, V]] =
    F.delay {
      val kstream = builder.stream(topic)(C.consumed(schemaRegistry))
      kstream.print(Printed.toSysOut[K, V].withLabel(topic))
      kstream
    }
}

final class KStreamOps[F[_], K, V](private val kStream: KStream[K, V]) extends AnyVal {

  def toF(
    topic: String,
    schemaRegistry: String
  )(implicit F: Sync[F], P: AvroRecordProduced[K, V]): F[Unit] =
    F.delay {
      kStream.print(Printed.toSysOut[K, V].withLabel(topic))
      kStream.to(topic)(P.produced(schemaRegistry))
    }
}

object dummy {
  final case class DummyValue(value: String)
  object DummyValue {
    final implicit val dummyValueEncoder: Encoder[DummyValue] =
      Encoder.gen[DummyValue]
    final implicit val dummyValueDecoder: Decoder[DummyValue] =
      Decoder.gen[DummyValue]
    final implicit val dummyValueSchemaFor: SchemaFor[DummyValue] =
      SchemaFor.gen[DummyValue]
    final implicit val dummyValueAvroCodec: AvroCodec[DummyValue] =
      AvroCodec.genericValue[DummyValue]
  }
}
