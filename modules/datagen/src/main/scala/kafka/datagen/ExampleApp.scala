package kafka.datagen

import zio._
import zio.console._
import zio.duration._
import zio.random.Random
import zio.test.magnolia.DeriveGen
import zio.test.{Gen, Sized}

// https://github.com/zio/zio/blob/master/test-magnolia-tests/shared/src/test/scala/zio/test/magnolia/DeriveGenSpec.scala
object ExampleApp extends App {

  final case class MyKey(uuid: java.util.UUID)
  object MyKey {
    val genMyKey: Gen[Random with Sized, MyKey] =
      DeriveGen[MyKey]
  }
  final case class MyValue(
    myBoolean: Boolean,
    myInt: Int,
    myString: String
  )
  object MyValue {
    val genMyValue: Gen[Random with Sized, MyValue] =
      DeriveGen[MyValue]
  }

  // sbt "datagen/runMain kafka.datagen.ExampleApp"
  override def run(args: List[String]): URIO[ZEnv, ExitCode] =
    MyKey.genMyKey.sample.zip(MyValue.genMyValue.sample)
      .foreach(sample => putStrLn(s"${sample._1.value}:${sample._2.value}"))
      .repeat(Schedule.recurs(3) && Schedule.spaced(1.second))
      .provideCustomLayer(Sized.live(1000))
      .exitCode
}
