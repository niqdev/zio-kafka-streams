package com.github.niqdev.schema

import com.github.niqdev.schema.GitHubEvent._
import com.github.niqdev.schema.repository._
import com.github.niqdev.schema.user._
import com.sksamuel.avro4s.{ Decoder, Encoder, SchemaFor }
import kafka.streams.serde.AvroCodec

object GitHubEvent extends GitHubEventInstances {

  /**
    * GitHub Event Key
    */
  final case class GitHubEventKey(
    userId: UserId
  )
  object GitHubEventKey {
    def fromUser(userKey: UserKey): GitHubEventKey =
      GitHubEventKey(userKey.id)
    def fromRepository(repositoryKey: RepositoryKey): GitHubEventKey =
      GitHubEventKey(repositoryKey.userId)
  }

  /**
    * GitHub Event Key
    */
  final case class GitHubEventValue(
    userName: UserName,
    repositoryName: RepositoryName,
    repositoryUrl: RepositoryUrl
  )
  object GitHubEventValue {
    def joinUserRepository(userValue: UserValue, repositoryValue: RepositoryValue): GitHubEventValue =
      GitHubEventValue(
        userName = userValue.name,
        repositoryName = repositoryValue.name,
        repositoryUrl = repositoryValue.url
      )
  }
}

sealed trait GitHubEventInstances {

  final implicit val gitHubEventKeyEncoder: Encoder[GitHubEventKey] =
    Encoder.gen[GitHubEventKey]
  final implicit val gitHubEventKeyDecoder: Decoder[GitHubEventKey] =
    Decoder.gen[GitHubEventKey]
  final implicit val gitHubEventKeySchemaFor: SchemaFor[GitHubEventKey] =
    SchemaFor.gen[GitHubEventKey]
  final implicit val gitHubEventKeyAvroCodec: AvroCodec[GitHubEventKey] =
    AvroCodec.genericKey[GitHubEventKey]

  final implicit val gitHubEventValueEncoder: Encoder[GitHubEventValue] =
    Encoder.gen[GitHubEventValue]
  final implicit val gitHubEventValueDecoder: Decoder[GitHubEventValue] =
    Decoder.gen[GitHubEventValue]
  final implicit val gitHubEventValueSchemaFor: SchemaFor[GitHubEventValue] =
    SchemaFor.gen[GitHubEventValue]
  final implicit val gitHubEventValueAvroCodec: AvroCodec[GitHubEventValue] =
    AvroCodec.genericValue[GitHubEventValue]
}
