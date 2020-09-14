package kafka.streams.serde

import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.scala.Serdes

trait Codec[T] {
  def serde: Serde[T]
}

// TODO circe/zio-json + xml-spac + Refined
object Codec {
  def apply[T](implicit ev: Codec[T]): Codec[T] = ev

  private[this] def instance[T](serdeInstance: Serde[T]): Codec[T] =
    new Codec[T] {
      override def serde: Serde[T] =
        serdeInstance
    }

  implicit val stringCodec: Codec[String] =
    instance(Serdes.String)
  implicit val longCodec: Codec[Long] =
    instance(Serdes.Long)
  implicit val javaLongCodec: Codec[java.lang.Long] =
    instance(Serdes.JavaLong)
  implicit val byteArrayCodec: Codec[Array[Byte]] =
    instance(Serdes.ByteArray)
  implicit val bytesCodec: Codec[org.apache.kafka.common.utils.Bytes] =
    instance(Serdes.Bytes)
  implicit val floatCodec: Codec[Float] =
    instance(Serdes.Float)
  implicit val javaFloatCodec: Codec[java.lang.Float] =
    instance(Serdes.JavaFloat)
  implicit val doubleCodec: Codec[Double] =
    instance(Serdes.Double)
  implicit val javaDoubleCodec: Codec[java.lang.Double] =
    instance(Serdes.JavaDouble)
  implicit val integerCodec: Codec[Int] =
    instance(Serdes.Integer)
  implicit val javaIntegerCodec: Codec[java.lang.Integer] =
    instance(Serdes.JavaInteger)
}
