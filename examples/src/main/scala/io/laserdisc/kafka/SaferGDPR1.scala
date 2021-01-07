package io.laserdisc.kafka

import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.scala.{ Serdes, StreamsBuilder }
import org.apache.kafka.streams.{ StreamsConfig, Topology }
import zio.UIO
import zio.kafka.streams.testkit.ZTestTopology
import zio.kafka.streams.{ KafkaStreamsTopology, SafeTopic, TopologySdk }
import zio.prelude.State
import zio.test.Assertion.equalTo
import zio.test.environment.TestEnvironment
import zio.test.{ DefaultRunnableSpec, TestFailure, ZSpec, suite, testM, _ }

import java.util.Properties
import zio.prelude.Subtype
import zio.kafka.streams.Extractor

object SaferGDPR1 extends TopologySdk {
  object Username extends Subtype[String]
  type Username = Username.Type
  object Password extends Subtype[String]
  type Password = Password.Type

  val config: Properties = {
    val jProperties = new Properties()
    jProperties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
    jProperties
  }

  val passwordFilter: (String, String) => Boolean = (k, _) => k.startsWith("p")
  val passwordExtractor: Extractor[String, String, Password] =
    Extractor(passwordFilter)(_.asInstanceOf[Password])
  val passwordSerde: Serde[Password]              = Serdes.String.asInstanceOf[Serde[Password]]
  val usernameFilter: (String, String) => Boolean = (k, _) => k.startsWith("u")
  val usernameExtractor: Extractor[String, String, Username] =
    Extractor(usernameFilter)(_.asInstanceOf[Username])
  val usernameSerde: Serde[Username] = Serdes.String.asInstanceOf[Serde[Username]]

  val newUsersIn: SafeTopic[String, String]    = SafeTopic("new-users-input", Serdes.String, Serdes.String)
  val newUsersOut: SafeTopic[String, Username] = SafeTopic("new-users-output", Serdes.String, usernameSerde)

  val topologyReader: State[StreamsBuilder, Topology] = for {
    input    <- streamFromSource(newUsersIn)
    branches <- safeBranch(input)(usernameExtractor, passwordExtractor)
    (usernames, passwords) = branches // cannot inline this due to missing `withFilter` in ZPure
    // ^-- try and switch these
    _        <- streamSinkTo(usernames, newUsersOut)
    topology <- toTopology
  } yield topology

  val topology: Topology = topologyReader.runResult(new StreamsBuilder())
}

object SaferGDPR1Spec extends DefaultRunnableSpec {
  private[this] val testLayer =
    ZTestTopology.testConfigLayer(true) >+> KafkaStreamsTopology.make(UIO(SaferGDPR1.topology))

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("newUsersSpec")(
      testM("topology") {
        for {
          outputValue <- ZTestTopology.driver.use { driver =>
            for {
              input  <- driver.createInput[String, String]("new-users-input")
              output <- driver.createOutput[String, String]("new-users-output")
              _      <- input.produce("u001", "Giovanni")
              _      <- input.produce("p001", "password1")
              value  <- output.consumeValue
            } yield value
          }
        } yield assert(outputValue)(equalTo("Giovanni"))
      }.provideSomeLayerShared(testLayer.mapError(TestFailure.fail))
    )
}
