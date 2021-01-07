package io.laserdisc.kafka

import org.apache.kafka.streams.scala.ImplicitConversions._
import org.apache.kafka.streams.scala.Serdes._
import org.apache.kafka.streams.scala.StreamsBuilder
import org.apache.kafka.streams.scala.kstream._
import org.apache.kafka.streams.{ StreamsConfig, Topology }
import zio.UIO
import zio.kafka.streams.KafkaStreamsTopology
import zio.kafka.streams.testkit.ZTestTopology
import zio.test.Assertion.equalTo
import zio.test._
import zio.test.environment.TestEnvironment

import java.util.Properties

object DangerousGDPR1 {
  val config: Properties = {
    val jProperties = new Properties()
    jProperties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
    jProperties
  }

  val builder: StreamsBuilder           = new StreamsBuilder()
  val newUsers: KStream[String, String] = builder.stream[String, String]("new-users-input")
  val branches: Array[KStream[String, String]] =
    newUsers.branch((k, _) => k.startsWith("u"), (k, _) => k.startsWith("p"))
  // array is 0 based
  val USERNAME = 1
  val PASSWORD = 2
  branches(USERNAME).to("new-users-output")

  val topology: Topology = builder.build()
}

object DangerousGDPR1Spec extends DefaultRunnableSpec {
  private[this] val testLayer =
    ZTestTopology.testConfigLayer(true) >+> KafkaStreamsTopology.make(UIO(DangerousGDPR1.topology))

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("newUsersSpec")(
      testM("topology") {
        for {
          outputValue <- ZTestTopology.driver.use { driver =>
            for {
              input  <- driver.createInput[String, String]("new-users-input")
              output <- driver.createOutput[String, String]("new-users-output")
              _      <- input.produce("p001", "Giovanni")
              _      <- input.produce("u001", "password1")
              value  <- output.consumeValue
            } yield value
          }
        } yield assert(outputValue)(equalTo("Giovanni"))
      }.provideSomeLayerShared(testLayer.mapError(TestFailure.fail))
    )
}
