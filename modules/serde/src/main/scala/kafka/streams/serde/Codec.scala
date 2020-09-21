package kafka.streams.serde

import org.apache.kafka.common.serialization.{ Deserializer, Serde, Serializer, Serdes => JSerdes }
import org.apache.kafka.streams.scala.Serdes

trait Codec[T] {
  def serde: Serde[T]

  // map Codec[T] to Codec[C]
  def cmap[C](s: C => T, d: T => Option[C])(
    implicit codec: Codec[T]
  ): Codec[C] =
    Codec.instance[C](
      JSerdes.serdeFrom(
        new Serializer[C] {
          override def serialize(topic: String, data: C): Array[Byte] =
            codec.serde.serializer().serialize(topic, s(data))
          override def configure(configs: java.util.Map[String, _], isKey: Boolean): Unit = ()
          override def close(): Unit                                                      = ()
        },
        new Deserializer[C] {
          // TODO ".get" throws exceptions
          // see org.apache.kafka.streams.scala.Serdes.fromFn
          // "orNull" requires an implicit instance of Null and [C >: Null] constraint
          // which doesn't compile with Refined and newtype
          override def deserialize(topic: String, data: Array[Byte]): C =
            Either.catchNonFatal(codec.serde.deserializer().deserialize(topic, data)).toOption.flatMap(d).get
          override def configure(configs: java.util.Map[String, _], isKey: Boolean): Unit = ()
          override def close(): Unit                                                      = ()
        }
      )
    )
}

// TODO circe/zio-json + xml-spac + Refined
object Codec {
  def apply[T](implicit ev: Codec[T]): Codec[T] = ev

  def instance[T](serdeInstance: Serde[T]): Codec[T] =
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
