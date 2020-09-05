package com.github.niqdev
package schema

import java.time.Instant
import java.util.UUID

import com.github.niqdev.schema.repository._
import com.github.niqdev.schema.user.UserId
import com.sksamuel.avro4s._
import eu.timepit.refined.api.{ Refined, RefinedTypeOps }
import eu.timepit.refined.string.Url
import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype
import kafka.streams.serde.AvroCodec

object repository extends RepositoryInstances {

  final type StringUrl = String Refined Url
  final object StringUrl extends RefinedTypeOps[StringUrl, String]

  @newtype case class RepositoryId(uuid: UUID)
  @newtype case class RepositoryName(string: NonEmptyString)
  @newtype case class RepositoryUrl(string: StringUrl)

  sealed trait Visibility
  @AvroName("PUBLIC")
  @AvroSortPriority(1)
  final case object Public extends Visibility
  @AvroName("PRIVATE")
  @AvroSortPriority(0)
  final case object Private extends Visibility

  /**
    * Repository Key
    */
  final case class RepositoryKey(
    eventId: EventId,
    id: RepositoryId,
    userId: UserId
  )

  /**
    * Repository Value
    */
  final case class RepositoryValue(
    name: RepositoryName,
    url: RepositoryUrl,
    isFork: Boolean,
    visibility: Visibility,
    tags: Map[NonEmptyString, String],
    createdAt: Instant,
    updatedAt: Instant
  )
}

sealed trait RepositoryInstances {
  import com.sksamuel.avro4s.refined._

  final implicit val repositoryIdEncoder: Encoder[RepositoryId] =
    Encoder.UUIDCodec.comap[RepositoryId](_.uuid)
  final implicit val repositoryIdDecoder: Decoder[RepositoryId] =
    Decoder.UUIDDecoder.map[RepositoryId](RepositoryId.apply)
  final implicit val repositoryIdSchemaFor: SchemaFor[RepositoryId] =
    SchemaFor[RepositoryId](SchemaFor.UUIDSchemaFor.schema)

  final implicit val repositoryNameEncoder: Encoder[RepositoryName] =
    Encoder.StringEncoder.comap[RepositoryName](_.string.value)
  final implicit val repositoryNameDecoder: Decoder[RepositoryName] =
    Decoder.StringDecoder.map[RepositoryName] { unsafeValue =>
      NonEmptyString
        .from(unsafeValue)
        .toOption
        .fold(sys.error(s"Unsupported type $unsafeValue"))(RepositoryName.apply)
    }
  final implicit val repositoryNameSchemaFor: SchemaFor[RepositoryName] =
    SchemaFor[RepositoryName](SchemaFor.StringSchemaFor.schema)

  final implicit val repositoryUrlEncoder: Encoder[RepositoryUrl] =
    Encoder.StringEncoder.comap[RepositoryUrl](_.string.value)
  final implicit val repositoryUrlDecoder: Decoder[RepositoryUrl] =
    Decoder.StringDecoder.map[RepositoryUrl] { unsafeValue =>
      StringUrl
        .from(unsafeValue)
        .toOption
        .fold(sys.error(s"Unsupported type $unsafeValue"))(RepositoryUrl.apply)
    }
  final implicit val repositoryUrlSchemaFor: SchemaFor[RepositoryUrl] =
    SchemaFor[RepositoryUrl](SchemaFor.StringSchemaFor.schema)

  final implicit val repositoryKeyEncoder: Encoder[RepositoryKey] =
    Encoder.gen[RepositoryKey]
  final implicit val repositoryKeyDecoder: Decoder[RepositoryKey] =
    Decoder.gen[RepositoryKey]
  final implicit val repositoryKeySchemaFor: SchemaFor[RepositoryKey] =
    SchemaFor.gen[RepositoryKey]
  final implicit val repositoryKeyAvroCodec: AvroCodec[RepositoryKey] =
    AvroCodec.genericKey[RepositoryKey]

  final implicit val repositoryValueEncoder: Encoder[RepositoryValue] =
    Encoder.gen[RepositoryValue]
  final implicit val repositoryValueDecoder: Decoder[RepositoryValue] =
    Decoder.gen[RepositoryValue]
  final implicit val repositoryValueSchemaFor: SchemaFor[RepositoryValue] =
    SchemaFor.gen[RepositoryValue]
  final implicit val repositoryValueAvroCodec: AvroCodec[RepositoryValue] =
    AvroCodec.genericValue[RepositoryValue]
}
