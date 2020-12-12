package io.laserdisc.kafka

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

object SafeGDPR1 extends TopologySdk {
  val config: Properties = {
    val jProperties = new Properties()
    jProperties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
    jProperties
  }

  val passwordFilter: (String, String) => Boolean = (k, _) => k.startsWith("p")
  val usernameFilter: (String, String) => Boolean = (k, _) => k.startsWith("u")

  val newUsersIn: SafeTopic[String, String]  = SafeTopic("new-users-input", Serdes.String, Serdes.String)
  val newUsersOut: SafeTopic[String, String] = SafeTopic("new-users-output", Serdes.String, Serdes.String)

  val topologyReader: State[StreamsBuilder, Topology] = for {
    input    <- streamFromSource(newUsersIn)
    branches <- branch(input)(usernameFilter, passwordFilter)
    _        <- streamSinkTo(branches(passwordFilter), newUsersOut)
    topology <- toTopology
  } yield topology

  val topology: Topology = topologyReader.runResult(new StreamsBuilder())
}

object SafeGDPR1Spec extends DefaultRunnableSpec {
  private[this] val testLayer =
    ZTestTopology.testConfigLayer(true) >+> KafkaStreamsTopology.make(UIO(SafeGDPR1.topology))

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
