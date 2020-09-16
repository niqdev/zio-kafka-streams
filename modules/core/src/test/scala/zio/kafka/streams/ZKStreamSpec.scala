package zio.kafka.streams

import zio._
import zio.test.Assertion._
import zio.test._
import zio.test.environment._

object ZKStreamSpec extends DefaultRunnableSpec {
  override def spec: ZSpec[TestEnvironment, Any] =
    suite("ZKStreamSpec")(
      testM("TODO") {
        assertM(Task.succeed("hello"))(equalTo("aaa"))
      }
    )
}
