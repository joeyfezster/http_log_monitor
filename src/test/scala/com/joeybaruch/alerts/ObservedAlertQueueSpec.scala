package com.joeybaruch.alerts

import com.joeybaruch.TestUtils._
import com.joeybaruch.alerts.AlertQueue.{Down, Up}
import com.joeybaruch.datamodel.Observer
import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ObservedAlertQueueSpec extends AnyFlatSpec with Matchers with BeforeAndAfter {

  var config: Config = _
  var oaq: ObservedAlertQueue = _
  var testReporter : TestAlertReporter = TestAlertReporter()

  before {
    config = ConfigFactory.load("test-conf")
      .withValue("alerts.raise-recover.avg.seconds", ConfigValueFactory.fromAnyRef(3))
      .withValue("alerts.requests.per-second.threshold", ConfigValueFactory.fromAnyRef(30))

    oaq = new ObservedAlertQueue(config)
    oaq.addObserver(testReporter)
  }

  behavior of "Observed Alert Queue"

  it should "report when alert is triggered and recovered" in {
    //                 expected:  (triggered, recovered)
    //                       expected:  (triggered, recovered)
    val ie1 = (oneSecWin(1L, 10L), (false, false))
    val ie2 = (oneSecWin(2L, 10L), (false, false))
    val ie3 = (oneSecWin(3L, 10L), (false, false))
    val ie4 = (oneSecWin(4L, 70L), (true, false))
    val ie5 = (oneSecWin(5L, 7L), (false, false))
    val ie6 = (oneSecWin(6L, 10L), (false, false))
    val ie7 = (oneSecWin(7L, 70L), (false, true))
    val inputExpectedSeq = Seq(ie1, ie2, ie3, ie4, ie5, ie6, ie7)

    //    leaving here for debugging
    //    oaq.enQ(ie1._1);
    //    (alertIsTriggered, alertIsRecovered) should be(ie1._2)
    //    oaq.enQ(ie2._1);
    //    (alertIsTriggered, alertIsRecovered) should be(ie2._2)
    //    oaq.enQ(ie3._1);
    //    (alertIsTriggered, alertIsRecovered) should be(ie3._2)
    //    oaq.enQ(ie4._1);
    //    (alertIsTriggered, alertIsRecovered) should be(ie4._2)
    //    alertIsTriggered = false
    //
    //    oaq.enQ(ie5._1);
    //    (alertIsTriggered, alertIsRecovered) should be(ie5._2)
    //    oaq.enQ(ie6._1);
    //    (alertIsTriggered, alertIsRecovered) should be(ie6._2)
    //    oaq.enQ(ie7._1);
    //    (alertIsTriggered, alertIsRecovered) should be(ie7._2)

    inputExpectedSeq.foreach {
      case (in, expectedTriggeredRecovered) =>
        oaq.enQueue(in)
        (in, testReporter.status) should be(in, expectedTriggeredRecovered)
        testReporter.reset()
    }
  }

  it should "report triggered and recovered even when a long time goes empty" in {
    //                 expected:  (triggered, recovered)
    val ie1 = (oneSecWin(1L, 10L), (false, false))
    val ie2 = (oneSecWin(2L, 10L), (false, false))
    val ie3 = (oneSecWin(3L, 10L), (false, false))
    val ie4 = (oneSecWin(4L, 70L), (true, false))
    val ie5 = (oneSecWin(30L, 7L), (false, true))
    val inputExpectedSeq = Seq(ie1, ie2, ie3, ie4, ie5)

    //    leaving here for debugging
    //    oaq.enQ(ie1._1);
    //    (alertIsTriggered, alertIsRecovered) should be(ie1._2)
    //    oaq.enQ(ie2._1);
    //    (alertIsTriggered, alertIsRecovered) should be(ie2._2)
    //    oaq.enQ(ie3._1);
    //    (alertIsTriggered, alertIsRecovered) should be(ie3._2)
    //    oaq.enQ(ie4._1);
    //    (alertIsTriggered, alertIsRecovered) should be(ie4._2)
    //    alertIsTriggered = false
    //
    //    oaq.enQ(ie5._1);
    //    (alertIsTriggered, alertIsRecovered) should be(ie5._2)

    inputExpectedSeq.foreach {
      case (in, expectedTriggeredRecovered) =>
        oaq.enQueue(in)
        (in, testReporter.status) should be(in, expectedTriggeredRecovered)
        testReporter.reset()
    }
  }

}
