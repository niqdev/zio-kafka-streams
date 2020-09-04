lazy val V = new {
  val avro4s    = "4.0.0"
  val confluent = "5.5.1"
  val kafka     = "2.5.1"
  val zio       = "1.0.1"
}

lazy val commonSettings = Seq(
  organization := "com.github.niqdev",
  scalaVersion := "2.13.3"
)

lazy val serde = project
  .in(file("modules/serde"))
  .settings(commonSettings)
  .settings(
    name := "kafka-streams-serde",
    resolvers ++= Seq(
      "confluent" at "https://packages.confluent.io/maven/"
    ),
    libraryDependencies ++= Seq(
      "org.apache.kafka"    %% "kafka-streams-scala"      % V.kafka,
      "io.confluent"         % "kafka-streams-avro-serde" % V.confluent,
      "com.sksamuel.avro4s" %% "avro4s-core"              % V.avro4s,
      "com.sksamuel.avro4s" %% "avro4s-refined"           % V.avro4s
    )
  )

lazy val core = project
  .in(file("modules/core"))
  .dependsOn(serde)
  .settings(commonSettings)
  .settings(
    name := "zio-kafka-streams",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % V.zio
    )
  )

lazy val examples = project
  .in(file("examples"))
  .dependsOn(core)
  .settings(commonSettings)
  .settings(
    name := "examples"
  )

lazy val root = project
  .in(file("."))
  .aggregate(core, serde, examples)
  .settings(
    name := "zio-kafka-streams-root",
    addCommandAlias("checkFormat", ";scalafmtCheckAll;scalafmtSbtCheck"),
    addCommandAlias("format", ";scalafmtAll;scalafmtSbt"),
    addCommandAlias("build", ";checkFormat;clean;test")
  )
