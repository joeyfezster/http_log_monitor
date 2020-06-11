package com.joeybaruch

import akka.actor.ActorSystem
import com.joeybaruch.TestUtils._
import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class HttpLogMonitorSpec extends AnyFlatSpec with Matchers with BeforeAndAfter {

  implicit val system: ActorSystem = ActorSystem()

  behavior of "http log monitor"
  it should "process a file" in {
    val config = ConfigFactory.load()
      .withValue("alerts.requests.per-second.threshold", ConfigValueFactory.fromAnyRef(10))

    HttpLogMonitor.runForFile(fullSampleFile, config)
    Thread.sleep(5 * 1000)
  }

}
