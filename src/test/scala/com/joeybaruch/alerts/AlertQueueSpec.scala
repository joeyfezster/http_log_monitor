package com.joeybaruch.alerts

import com.joeybaruch.TestUtils._
import com.joeybaruch.alerts.AlertQueue.{Down, Up}
import com.joeybaruch.windowing.EventsWindow
import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class AlertQueueSpec extends AnyFlatSpec with Matchers with BeforeAndAfter {

  var aq: AlertQueue = _
  var config: Config = _
  val timeSpan = 3L
  val threshold = 30

  before {
    config = ConfigFactory.load("test-conf")
      .withValue("alerts.raise-recover.avg.seconds", ConfigValueFactory.fromAnyRef(timeSpan))
      .withValue("alerts.requests.per-second.threshold", ConfigValueFactory.fromAnyRef(threshold))
    aq = new AlertQueue(config)
  }

  behavior of "alerting and recovering"
  it should "alert when crossing the threshold" in {

    //moving avg value: (10, 15, 20, 30)
    aq.enQueue(e1)
    aq.alertStatus should be(Down)

    aq.enQueue(e2)
    aq.alertStatus should be(Down)

    aq.enQueue(e3)
    aq.alertStatus should be(Down)

    aq.enQueue(e4)
    aq.alertStatus should be(Up)
  }

  it should "keep alert status even after going under the threshold so long as enough time has not passed" in {
    //moving avg value: (10, 10, 10, 30, 29, 29, 29) -> alerts: (-,-,-,+,+,+,-) //assuming threshold = 30
    val ie1 = (oneSecWin(1L, 10L), Down)
    val ie2 = (oneSecWin(2L, 10L), Down)
    val ie3 = (oneSecWin(3L, 10L), Down)
    val ie4 = (oneSecWin(4L, 70L), Up)
    val ie5 = (oneSecWin(5L, 7L), Up)
    val ie6 = (oneSecWin(6L, 10L), Up)
    val ie7 = (oneSecWin(7L, 70L), Down)
    val inputExpectedSeq = Seq(ie1, ie2, ie3, ie4, ie5, ie6, ie7)

    //    leaving here for debugging
    //    aq.enQ(ie1._1); aq.alertStatus should be(ie1._2)
    //    aq.enQ(ie2._1); aq.alertStatus should be(ie2._2)
    //    aq.enQ(ie3._1); aq.alertStatus should be(ie3._2)
    //    aq.enQ(ie4._1); aq.alertStatus should be(ie4._2)
    //    aq.enQ(ie5._1); aq.alertStatus should be(ie5._2)
    //    aq.enQ(ie6._1); aq.alertStatus should be(ie6._2)
    //    aq.enQ(ie7._1); aq.alertStatus should be(ie7._2)

    inputExpectedSeq.foreach { case (in, status) => aq.enQueue(in); (in, aq.alertStatus) should be(in, status) }
  }

  it should "not recover alert if re-triggered even at the last second" in {
    //moving avg value: (10, 10, 10, 30, 29, 29, 30, 29, 29) -> alerts: (-,-,-,+,+,+,+,+,+) //assuming threshold = 30
    val ie1 = (oneSecWin(1L, 10L), Down)
    val ie2 = (oneSecWin(2L, 10L), Down)
    val ie3 = (oneSecWin(3L, 10L), Down)
    val ie4 = (oneSecWin(4L, 70L), Up)
    val ie5 = (oneSecWin(5L, 7L), Up)
    val ie6 = (oneSecWin(6L, 10L), Up)
    val ie7 = (oneSecWin(7L, 73L), Up)
    val ie8 = (oneSecWin(8L, 4L), Up)
    val ie9 = (oneSecWin(9L, 10L), Up)
    val inputExpectedSeq = Seq(ie1, ie2, ie3, ie4, ie5, ie6, ie7, ie8, ie9)

    //    leaving here for debugging
    //    aq.enQ(ie1._1); aq.alertStatus should be(ie1._2)
    //    aq.enQ(ie2._1); aq.alertStatus should be(ie2._2)
    //    aq.enQ(ie3._1); aq.alertStatus should be(ie3._2)
    //    aq.enQ(ie4._1); aq.alertStatus should be(ie4._2)
    //    aq.enQ(ie5._1); aq.alertStatus should be(ie5._2)
    //    aq.enQ(ie6._1); aq.alertStatus should be(ie6._2)
    //    aq.enQ(ie7._1); aq.alertStatus should be(ie7._2)
    //    aq.enQ(ie8._1); aq.alertStatus should be(ie8._2)
    //    aq.enQ(ie9._1); aq.alertStatus should be(ie9._2)

    inputExpectedSeq.foreach { case (in, status) => aq.enQueue(in); (in, aq.alertStatus) should be(in, status) }
  }

  it should "return the correct time when a status was changed" in {
    //moving avg value: (10, 10, 10, 30, 29, 29, 29, 30, ..., 10) -> alerts: (-,-,-,+,+,+,-,+,...,-) //assuming threshold = 30
    val ie1 = oneSecWin(1L, 10L) // Down
    val ie2 = oneSecWin(2L, 10L) // Down
    val ie3 = oneSecWin(3L, 10L) // Down
    val ie4 = oneSecWin(4L, 70L) // Up
    val ie5 = oneSecWin(5L, 7L) // Up
    val ie6 = oneSecWin(6L, 10L) // Up
    val ie7 = oneSecWin(7L, 70L) // Down
    val ie8 = oneSecWin(8L, 10L) // Up
    val ie9 = oneSecWin(70L, 10L) // Down   -> long time after

    aq.enQueue(ie1) //Down
    aq.enQueue(ie2) //Down
    aq.enQueue(ie3) //Down
    aq.enQueue(ie4) //Up    ***
    aq.lastStatusChangeTime should be(ie4.winStartTime)

    aq.enQueue(ie5) //Up
    aq.enQueue(ie6) //Up
    aq.enQueue(ie7) //Down  ***
    aq.lastStatusChangeTime should be(ie7.winStartTime)

    aq.enQueue(ie8) //Up
    aq.enQueue(ie9) //Down  *** Long time
    aq.lastStatusChangeTime should be(ie8.winStartTime + timeSpan)
  }

  behavior of "enqueuing the alert queue"
  it should "update value as it ramps up" in {
    aq.enQueue(e1)
    aq.averageValue should be(e1.eventCount.toDouble) //10

    aq.enQueue(e2)
    aq.averageValue should be((e1.eventCount + e2.eventCount).toDouble / 2) //15

    aq.enQueue(e3)
    aq.averageValue should be((e1.eventCount + e2.eventCount + e3.eventCount).toDouble / 3) //20
  }

  it should "update value as sliding window" in {
    Seq(e1, e2, e3, e4).foreach(aq.enQueue(_))

    aq.averageValue should be((e2.eventCount + e3.eventCount + e4.eventCount).toDouble / 3)
  }

  it should "update value with skipped time" in {
    val skip1 = oneSecWin(e4.winEndTime + 2L, 60L)
    val skipOneSec = Seq(e1, e2, e3, e4, skip1)
    skipOneSec.foreach(aq.enQueue(_))

    aq.averageValue should be((e4.eventCount + skip1.eventCount).toDouble / 3)
  }

  it should "evacuate when value skips all time" in {
    val skipAll = Seq(e1, e4)
    skipAll.foreach(aq.enQueue(_))

    aq.averageValue should be(e4.eventCount.toDouble / 1)
    aq.spanInQueue should be(1L)
  }

  it should "except if queried when empty" in {
    intercept[IndexOutOfBoundsException] {
      aq.averageValue
    }
    intercept[IndexOutOfBoundsException] {
      aq.lastStatusChangeTime
    }
  }

  it should "not accept aggregated metrics with more than one second worth" in {
    intercept[IllegalArgumentException] {
      aq.enQueue(e1 + e2)
    }
  }

  it should "not accept late data" in {
    intercept[IllegalArgumentException] {
      val lateData = Seq(e1, e1)
      lateData.foreach(aq.enQueue(_))
    }
  }

  lazy val e1: EventsWindow = oneSecWin(1L, 10L)
  lazy val e2: EventsWindow = oneSecWin(2L, 20L)
  lazy val e3: EventsWindow = oneSecWin(3L, 30L)
  lazy val e4: EventsWindow = oneSecWin(4L, 40L)


}
