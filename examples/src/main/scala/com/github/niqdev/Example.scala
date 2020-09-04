package com.github.niqdev

import zio._
import zio.console._

// sbt -jvm-debug 5005 "examples/runMain com.github.niqdev.Example"
object Example extends App {

  override def run(args: List[String]): URIO[ZEnv, ExitCode] =
    putStrLn("hello").exitCode
}
