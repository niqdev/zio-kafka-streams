package com.github.niqdev

import java.util.UUID

import com.sksamuel.avro4s._
import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype

package object schema {

  final implicit val snake: FieldMapper = SnakeCase

  @newtype case class EventId(uuid: UUID)

  // TODO newtype support (replace EventId, UserId, RepositoryId instances)
  final implicit val eventIdEncoder: Encoder[EventId] =
    Encoder.UUIDCodec.comap[EventId](_.uuid)
  final implicit val userIdDecoder: Decoder[EventId] =
    Decoder.UUIDDecoder.map[EventId](EventId.apply)
  final implicit val userIdSchemaFor: SchemaFor[EventId] =
    SchemaFor[EventId](SchemaFor.UUIDSchemaFor.schema)

  // TODO commons?
  final implicit def nesMapEncoder[T: Encoder]: Encoder[Map[NonEmptyString, T]] =
    Encoder.mapEncoder[T].comap(_.map { case (k, v) => k.value -> v })
  final implicit def nesMapDecoder[A: Decoder]: Decoder[Map[NonEmptyString, A]] =
    Decoder.mapDecoder[A].map {
      _.map {
        case (NonEmptyString(key), value) => key -> value
        case (_, value)                   => sys.error(s"Unsupported type $value")
      }
    }
  final implicit def nesMapSchemaFor[A: SchemaFor]: SchemaFor[Map[NonEmptyString, A]] =
    SchemaFor.mapSchemaFor[A].map[Map[NonEmptyString, A]](identity)
}
