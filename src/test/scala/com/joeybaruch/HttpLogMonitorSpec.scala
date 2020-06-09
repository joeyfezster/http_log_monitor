package com.joeybaruch

import akka.actor.ActorSystem
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import com.joeybaruch.TestUtils._

class HttpLogMonitorSpec extends AnyFlatSpec with Matchers with BeforeAndAfter {

  implicit val system: ActorSystem = ActorSystem()

  behavior of "http log monitor"
  it should "process a file" in {
    HttpLogMonitor.runForFile(fullSampleFile)
  }

}
