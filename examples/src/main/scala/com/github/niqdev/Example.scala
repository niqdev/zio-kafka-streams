package com.github.niqdev

import zio._
import zio.console._

object Example extends App {

  override def run(args: List[String]): URIO[ZEnv, ExitCode] =
    putStrLn("hello").exitCode
}
