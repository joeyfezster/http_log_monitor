package com.joeybaruch.alerts

import com.joeybaruch.alerts.AlertQueue.{AlertToggle, Down, Element, Up}
import com.joeybaruch.windowing.EventsWindow
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable

class AlertQueue(config: Config) extends LazyLogging {
  private val spanInSeconds = config.getInt("alerts.raise-recover.avg.seconds")
  private val threshold = config.getDouble("alerts.requests.per-second.threshold")


  private val queue = new mutable.Queue[Element]
  private var cumulativeValue: Long = 0
  private var alertingElementsInQueue: Int = 0
  private var lastTriggeringTime: Long = _


  def alertStatus: AlertToggle = {
    verifyNonEmpty()
    if (alertingElementsInQueue > 0) Up else Down
  }

  def earliestTime: Long = {
    verifyNonEmpty()
    queue.head.win.winStartTime
  }

  def latestTime: Long = {
    verifyNonEmpty()
    queue.last.win.winStartTime
  }

  def spanInQueue: Long = latestTime - earliestTime + 1

  def averageValue: Double = {
    verifyNonEmpty()
    val avg = cumulativeValue.toDouble / spanInQueue
    logger.debug(s"average: $avg, cumulative: $cumulativeValue, span: $spanInQueue")
    avg
  }

  def isEmpty: Boolean = queue.isEmpty

  def lastStatusChangeTime: Long = {
    alertStatus match {
      case Up => lastTriggeringTime
      case Down => scala.math.min(latestTime, lastTriggeringTime + spanInSeconds.toLong)
    }
  }

  def enQueue(element: EventsWindow): Unit = {
    require(element.winEndTime == element.winStartTime, "Alert Queue requires exactly one second aggregations")
    require(this.isEmpty || element.winStartTime > latestTime,
      s"Event time ${element.winStartTime} is later than the queue's latest event received: $latestTime")

    logger.debug(s"enqueuing $element")
    queue.addOne(Element(element, Down))
    cumulativeValue += element.eventCount

    dequeueOverflowingEvents()

    updateIfAlert(element)
  }

  private def dequeueOverflowingEvents(): Unit = {
    logger.debug(s"(latest -> earliest): ($latestTime -> $earliestTime)\t" +
      s"(spanInQueue vs requiredSpan)): ($spanInQueue vs $spanInSeconds)")
    while (spanInQueue > spanInSeconds) deQ()
  }

  private def deQ(): Element = {
    logger.debug("pop")
    val popped = queue.dequeue()

    cumulativeValue -= popped.win.eventCount
    if (popped.alertToggle equals Up) {
      alertingElementsInQueue -= 1
    }
    popped
  }


  private def updateIfAlert(element: EventsWindow): Unit = {
    val newAvg = averageValue
    if (newAvg >= threshold) {
      logger.debug(s"alerting element - (t: threshold, cv: cumulativeValue, siq: spanInQueue, elem: element)" +
        s" (t: $threshold, cv: $cumulativeValue, tsn: $spanInQueue, elem: $element)")
      queue.last.alertToggle = Up
      lastTriggeringTime = queue.last.win.winStartTime
      alertingElementsInQueue += 1
    }
  }

  private def verifyNonEmpty(): Unit = {
    if (queue.isEmpty) throw new IndexOutOfBoundsException("The Alert Queue is currently empty")
  }

}

object AlertQueue {

  sealed trait AlertToggle {}

  case object Up extends AlertToggle

  case object Down extends AlertToggle

  case class Element(win: EventsWindow, var alertToggle: AlertToggle)

}
