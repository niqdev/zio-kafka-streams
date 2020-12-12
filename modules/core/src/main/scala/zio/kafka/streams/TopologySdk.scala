package zio.kafka.streams

import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.scala.StreamsBuilder
import org.apache.kafka.streams.scala.kstream.{ Consumed, KStream, Produced }
import zio.prelude.State
import zio.prelude.fx.ZPure

trait TopologySdk {
  def streamFromSource[K, V](
    sourceSafeTopic: SafeTopic[K, V]
  ): State[StreamsBuilder, KStream[K, V]] = {
    val consumed = Consumed.`with`(sourceSafeTopic.keySerde, sourceSafeTopic.valueSerde)
    ZPure
      .get[StreamsBuilder]
      .map(_.stream[K, V](sourceSafeTopic.topicName)(consumed))
  }

  def streamSinkTo[K, V](
    stream: KStream[K, V],
    sinkSafeTopic: SafeTopic[K, V]
  ): ZPure[StreamsBuilder, StreamsBuilder, Any, Nothing, Unit] = {
    val produced = Produced.`with`(sinkSafeTopic.keySerde, sinkSafeTopic.valueSerde)
    ZPure
      .unit[StreamsBuilder]
      .as(stream.to(sinkSafeTopic.topicName)(produced))
  }

  def branch[K, V](stream: KStream[K, V])(
    predicate1: (K, V) => Boolean,
    predicate2: (K, V) => Boolean
  ): State[StreamsBuilder, Map[(K, V) => Boolean, KStream[K, V]]] =
    ZPure.unit[StreamsBuilder].as {
      val branches = stream.branch(predicate1, predicate2)
      Map(
        predicate1 -> branches(0),
        predicate2 -> branches(1)
      )
    }

  def safeBranch[K, Vin, Vout1 <: Vin, Vout2 <: Vin](stream: KStream[K, Vin])(
    extractor1: Extractor[K, Vin, Vout1],
    extractor2: Extractor[K, Vin, Vout2]
  ): State[StreamsBuilder, (KStream[K, Vout1], KStream[K, Vout2])] =
    ZPure.unit[StreamsBuilder].as {
      val branches = stream.branch(extractor1.predicate, extractor2.predicate)
      val stream1  = branches(0).mapValues(extractor1.downcaster)
      val stream2  = branches(1).mapValues(extractor2.downcaster)
      (stream1, stream2)
    }

  def streamMapValues[K, Vin, Vout](stream: KStream[K, Vin])(
    f: Vin => Vout
  ): State[StreamsBuilder, KStream[K, Vout]] =
    ZPure.unit[StreamsBuilder].as(stream.mapValues(f))

  def toTopology: State[StreamsBuilder, Topology] = ZPure.get.map(_.build())
}

final case class SafeTopic[K, V](topicName: String, keySerde: Serde[K], valueSerde: Serde[V])
final class Extractor[K, Vin, Vout <: Vin] private (
  val predicate: (K, Vin) => Boolean,
  val downcaster: Vin => Vout
)
object Extractor {
  def apply[K, Vin, Vout <: Vin](predicate: (K, Vin) => Boolean)(downcaster: Vin => Vout) =
    new Extractor[K, Vin, Vout](predicate, downcaster)
}
