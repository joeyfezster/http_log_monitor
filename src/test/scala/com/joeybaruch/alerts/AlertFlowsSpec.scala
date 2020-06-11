package com.joeybaruch.alerts

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.joeybaruch.TestUtils._
import com.joeybaruch.windowing.EventsWindow
import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Success

class AlertFlowsSpec extends AnyFlatSpec with Matchers with BeforeAndAfter {
  implicit val system: ActorSystem = ActorSystem()

  var config: Config = _
  var oaq: ObservedAlertQueue = _
  var testReporter: TestAlertReporter = _

  before {
    config = ConfigFactory.load("test-conf")
      .withValue("alerts.raise-recover.avg.seconds", ConfigValueFactory.fromAnyRef(3))
      .withValue("alerts.requests.per-second.threshold", ConfigValueFactory.fromAnyRef(30))

    oaq = new ObservedAlertQueue(config)
    testReporter = TestAlertReporter()
    oaq.addObserver(testReporter)
  }

  behavior of "processing alerts"
  it should "let it flow" in {
    //                 expected:  (triggered, recovered)
    val ie1 = oneSecWin(1L, 10L)
    val ie2 = oneSecWin(2L, 10L)
    val ie3 = oneSecWin(3L, 10L)
    val ie4 = oneSecWin(4L, 70L)
    val ie5 = oneSecWin(5L, 7L)
    val ie6 = oneSecWin(6L, 10L)
    val ie7 = oneSecWin(7L, 70L)
    val inputExpectedSeq = Seq(ie1, ie2, ie3, ie4, ie5, ie6, ie7)

    val flowUnderTest: Flow[EventsWindow, Nothing, NotUsed] = AlertFlows.processAlerts(oaq)
    val future = Source(inputExpectedSeq).via(flowUnderTest).runWith(Sink.ignore)
    Await.result(future, 3.seconds)

    future.value.get should be(Success(Done))
  }

}
