package com.joeybaruch.alerts

import com.joeybaruch.alerts.AlertQueue.{Down, Up}
import com.joeybaruch.datamodel.AggregatedMetrics.BaseAggMetrics
import com.joeybaruch.datamodel.Observer
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import com.joeybaruch.TestUtils._

class ObservedAlertQueueSpec extends AnyFlatSpec with Matchers with BeforeAndAfter {

  var alertIsTriggered: Boolean = _
  var alertIsRecovered: Boolean = _
  var config: Config = _
  var oaq: ObservedAlertQueue = _
  var testReporter = new TestAlertReporter()

  before {
    alertIsTriggered = false
    alertIsRecovered = false
    //assuming threshold = 30, and alerting/recovering after 3 seconds
    config = ConfigFactory.load("test-conf")

    oaq = new ObservedAlertQueue(config)
    oaq.addObserver(testReporter)
  }

  behavior of "Observed Alert Queue"

  it should "report when alert is triggered and recovered" in {
    //                            (triggered, recovered)
    val ie1 = (bag(1L, 10L), (false, false))
    val ie2 = (bag(2L, 10L), (false, false))
    val ie3 = (bag(3L, 10L), (false, false))
    val ie4 = (bag(4L, 70L), (true, false))
    val ie5 = (bag(5L, 7L), (false, false))
    val ie6 = (bag(6L, 10L), (false, false))
    val ie7 = (bag(7L, 70L), (false, true))
    val inputExpectedSeq = Seq(ie1, ie2, ie3, ie4, ie5, ie6, ie7)
//
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
      case (in, expectedTriggeredRecovered) => {
        oaq.enQ(in)
        (in, (alertIsTriggered, alertIsRecovered)) should be(in, expectedTriggeredRecovered)
        alertIsTriggered = false
        alertIsRecovered = false
      }
    }

  }

  class TestAlertReporter extends Observer[AlertQueue] {
    def receiveUpdate(alertQueue: AlertQueue) = {
      alertQueue.alertStatus match {
        case Up => alertIsTriggered = true
        case Down => alertIsRecovered = true
      }
    }
  }


}
