package com.github.niqdev
package schema

import java.time.Instant
import java.util.UUID

import com.github.niqdev.schema.user._
import com.sksamuel.avro4s.{ Decoder, Encoder, SchemaFor }
import enumeratum.values.{ StringEnum, StringEnumEntry }
import eu.timepit.refined.W
import eu.timepit.refined.api.{ Refined, RefinedTypeOps }
import eu.timepit.refined.string.MatchesRegex
import io.estatico.newtype.macros.newtype
import kafka.streams.serde.AvroCodec

object user extends UserInstances {

  final val AlphanumericRegex = W("""^[a-zA-Z0-9]+$""")
  final type Alphanumeric = String Refined MatchesRegex[AlphanumericRegex.T]
  final object Alphanumeric extends RefinedTypeOps[Alphanumeric, String]

  @newtype case class UserId(uuid: UUID)
  @newtype case class UserName(string: Alphanumeric)

  sealed abstract class Ownership(val value: String) extends StringEnumEntry
  case object Ownership extends StringEnum[Ownership] {
    val values = findValues

    case object User         extends Ownership("user")
    case object Organization extends Ownership("organization")
  }

  /**
    * User Key
    */
  final case class UserKey(
    eventId: EventId,
    id: UserId
  )

  /**
    * User Value
    */
  final case class UserValue(
    name: UserName,
    ownership: Ownership,
    createdAt: Instant,
    updatedAt: Instant
  )
}

sealed trait UserInstances {

  final implicit val userIdEncoder: Encoder[UserId] =
    Encoder.UUIDCodec.comap[UserId](_.uuid)
  final implicit val userIdDecoder: Decoder[UserId] =
    Decoder.UUIDDecoder.map[UserId](UserId.apply)
  final implicit val userIdSchemaFor: SchemaFor[UserId] =
    SchemaFor[UserId](SchemaFor.UUIDSchemaFor.schema)

  final implicit val userNameEncoder: Encoder[UserName] =
    Encoder.StringEncoder.comap[UserName](_.string.value)
  final implicit val userNameDecoder: Decoder[UserName] =
    Decoder.StringDecoder.map[UserName] { unsafeValue =>
      Alphanumeric
        .from(unsafeValue)
        .toOption
        .fold(sys.error(s"Unsupported type $unsafeValue"))(UserName.apply)
    }
  final implicit val userNameSchemaFor: SchemaFor[UserName] =
    SchemaFor[UserName](SchemaFor.StringSchemaFor.schema)

  final implicit val ownershipEncoder: Encoder[Ownership] =
    Encoder.StringEncoder.comap[Ownership](_.value)
  final implicit val ownershipDecoder: Decoder[Ownership] =
    Decoder.StringDecoder.map[Ownership] { unsafeValue =>
      Ownership
        .valuesToEntriesMap
        .get(unsafeValue)
        .fold(sys.error(s"Unsupported type $unsafeValue"))(value => value)
    }
  final implicit val ownershipSchemaFor: SchemaFor[Ownership] =
    SchemaFor[Ownership](SchemaFor.StringSchemaFor.schema)

  final implicit val userKeyAvroCodec: AvroCodec[UserKey] =
    AvroCodec.genericKey[UserKey]
  final implicit val userValueAvroCodec: AvroCodec[UserValue] =
    AvroCodec.genericValue[UserValue]
}
