package kafka.streams.serde

import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.scala.Serdes

trait Codec[T] {
  def serde: Serde[T]
}

// TODO circe/zio-json + xml-spac + all common serdes + Refined
object Codec {
  def apply[T](implicit ev: Codec[T]): Codec[T] = ev

  implicit val stringCodec: Codec[String] =
    new Codec[String] {
      override def serde: Serde[String] =
        Serdes.String
    }
}
