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

object DangerousGDPR2 {
  val config: Properties = {
    val jProperties = new Properties()
    jProperties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
    jProperties
  }

  val builder: StreamsBuilder           = new StreamsBuilder()
  val newUsers: KStream[String, String] = builder.stream[String, String]("new-users-input")
  val branches: Array[KStream[String, String]] =
    newUsers.branch((k, _) => k.startsWith("u"), (k, _) => k.startsWith("c"))
  val USERNAME                                            = 0
  def securelyMappingUserForOutput(user: String): String  = identity(user)
  val CREDIT_CARD                                         = 1
  def securelyMappingCardForOutput(ccard: String): String = "************" + ccard.substring(12)
  val processUser: KStream[String, String] =
    branches(USERNAME).mapValues(username => securelyMappingUserForOutput(username))
  val maskedCC: KStream[String, String] =
    branches(CREDIT_CARD).mapValues(creditCard => securelyMappingUserForOutput(creditCard))
  maskedCC.to("new-users-output")

  val topology: Topology = builder.build()
}

object DangerousGDPR2Spec extends DefaultRunnableSpec {
  private[this] val testLayer =
    ZTestTopology.testConfigLayer(true) >+> KafkaStreamsTopology.make(UIO(DangerousGDPR2.topology))

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("newUsersSpec")(
      testM("topology") {
        for {
          outputValue <- ZTestTopology.driver.use { driver =>
            for {
              input  <- driver.createInput[String, String]("new-users-input")
              output <- driver.createOutput[String, String]("new-users-output")
              _      <- input.produce("u001", "Giovanni")
              _      <- input.produce("c001", "5483987343872038")
              value  <- output.consumeValue
            } yield value
          }
        } yield assert(outputValue)(equalTo("************2038"))
      }.provideSomeLayerShared(testLayer.mapError(TestFailure.fail))
    )
}
