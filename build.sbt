lazy val V = new {
  val avro4s    = "4.0.1"
  val confluent = "6.0.0"
  val kafka     = "2.6.0"
  val zio       = "1.0.3"

  // examples
  val catsEffect = "2.2.0"
  val enumeratum = "1.6.1"
  val logback    = "1.2.3"
  val newtype    = "0.4.4"
  val refined    = "0.9.17"
  val zioConfig  = "1.0.0-RC29"
  val zioLogging = "0.5.3"
}

lazy val commonSettings = Seq(
  organization := "com.github.niqdev",
  scalaVersion := "2.13.3",
  scalacOptions ++= Seq(
    "-encoding",
    "UTF-8",
    "-unchecked",
    "-explaintypes",
    "-language:existentials",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-Xlint",
    "-Wconf:any:error"
  )
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
      "com.sksamuel.avro4s" %% "avro4s-core"              % V.avro4s,
      "io.confluent"         % "kafka-streams-avro-serde" % V.confluent,
      "org.apache.kafka"    %% "kafka-streams-scala"      % V.kafka
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

lazy val testkit = project
  .in(file("modules/testkit"))
  .dependsOn(core)
  .settings(commonSettings)
  .settings(
    name := "zio-kafka-streams-testkit",
    libraryDependencies ++= Seq(
      "org.apache.kafka" % "kafka-streams-test-utils" % V.kafka,
      "dev.zio"         %% "zio-test"                 % V.zio
    )
  )

lazy val examples = project
  .in(file("examples"))
  .dependsOn(core)
  .settings(commonSettings)
  .settings(
    name := "examples",
    scalacOptions ++= Seq(
      "-Ymacro-annotations"
    ),
    libraryDependencies ++= Seq(
      "com.sksamuel.avro4s" %% "avro4s-refined"     % V.avro4s,
      "org.typelevel"       %% "cats-effect"        % V.catsEffect,
      "com.beachape"        %% "enumeratum"         % V.enumeratum,
      "io.estatico"         %% "newtype"            % V.newtype,
      "eu.timepit"          %% "refined"            % V.refined,
      "dev.zio"             %% "zio-logging"        % V.zioLogging,
      "dev.zio"             %% "zio-config"         % V.zioConfig,
      "dev.zio"             %% "zio-config-refined" % V.zioConfig,
      "ch.qos.logback"       % "logback-classic"    % V.logback % Runtime
    )
  )

lazy val tests = project
  .in(file("modules/tests"))
  .dependsOn(testkit, examples)
  .settings(commonSettings)
  .settings(
    name := "tests",
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),
    libraryDependencies ++= Seq(
      "dev.zio"       %% "zio-test-sbt"    % V.zio     % Test,
      "ch.qos.logback" % "logback-classic" % V.logback % Runtime
    )
  )

lazy val root = project
  .in(file("."))
  .aggregate(serde, core, testkit, tests, examples)
  .settings(
    name := "zio-kafka-streams-root",
    addCommandAlias("checkFormat", ";scalafmtCheckAll;scalafmtSbtCheck"),
    addCommandAlias("format", ";scalafmtAll;scalafmtSbt"),
    addCommandAlias("build", ";checkFormat;clean;test")
  )
