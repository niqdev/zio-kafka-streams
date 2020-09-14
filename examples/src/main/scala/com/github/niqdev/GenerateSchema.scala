package com.github.niqdev

import java.nio.file.{ Files, Paths }

import com.github.niqdev.schema.repository._
import com.github.niqdev.schema.user._
import com.sksamuel.avro4s.{ AvroSchema, SchemaFor }
import zio._
import zio.console._

// TODO sbt-plugin
object GenerateSchema extends App {

  override def run(args: List[String]): URIO[ZEnv, ExitCode] =
    outputSchemas.exitCode

  private[this] def outputSchema[T: SchemaFor](schemaName: String): RIO[Console, Unit] = {
    val path   = s"./local/schema/$schemaName"
    val schema = AvroSchema[T].toString(true)

    Task.effect(Files.writeString(Paths.get(path), schema)) *>
      putStrLn(s"""
        |Generate schema:
        |path: $path
        |$schema
        |""".stripMargin)
  }

  private[this] def outputSchemas: RIO[Console, Unit] =
    outputSchema[UserKey]("example.user.v1-key.avsc") *>
      outputSchema[UserValue]("example.user.v1-value.avsc") *>
      outputSchema[RepositoryKey]("example.repository.v1-key.avsc") *>
      outputSchema[RepositoryValue]("example.repository.v1-value.avsc")
}
