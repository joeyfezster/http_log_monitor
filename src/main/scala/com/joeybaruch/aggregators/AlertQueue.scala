package com.joeybaruch.aggregators

import com.joeybaruch.aggregators.AlertQueue.{AlertToggle, Down, Element, Up}
import com.joeybaruch.datamodel.AggregatedMetrics.BaseAggMetrics
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable

class AlertQueue(config: Config) extends LazyLogging {
  private val spanInSeconds = config.getInt("alerts.raise-recover.avg.seconds")
  private val threshold = config.getDouble("alerts.requests.per-second.threshold")


  private val queue = new mutable.Queue[Element]
  private var cumulativeValue: Long = 0
  private var alertingElementsInQueue: Int = 0

  def alertStatus: AlertToggle = {
    verifyNonEmpty
    if (alertingElementsInQueue > 0) Up else Down
  }

  def earliestTime: Long = queue.headOption.fold(Long.MinValue)(_.bag.earliestTimestamp)

  def latestTime: Long = queue.lastOption.fold(Long.MinValue)(_.bag.earliestTimestamp)

  def spanInQueue: Long = latestTime - earliestTime + 1

  def averageValue: Double = {
    verifyNonEmpty
    val avg = cumulativeValue.toDouble / spanInQueue
    logger.debug(s"average: $avg, cumulative: $cumulativeValue, span: $spanInQueue")
    avg
  }

  def isEmpty: Boolean = queue.isEmpty


  def enQ(element: BaseAggMetrics) = {
    require(element.latestTimestamp == element.earliestTimestamp, "Alert Queue requires exactly one second aggregations")
    require(element.earliestTimestamp > latestTime,
      s"Event time ${element.earliestTimestamp} is later than the queue's latest event received: $latestTime")

    logger.debug(s"enqueuing $element")
    queue.addOne(Element(element, Down))
    cumulativeValue += element.eventCount

    deQUntilSpan

    updateIfAlert(element)
  }


  /**
   * This method is not as efficient as it could be, yet it is simple to implement and understand.
   *
   * This method is called more than once when the upstream log file has "wall clock" seconds without logs, otherwise
   * it'll just evacuate at most one element.
   *
   * In the cases of wall-clock time passing without logs, this method will take linear time by the size of the queue,
   * but by configuration, the queue is designed to hold in the order of hundreds of events, which is why this is an acceptable
   * tradeoff.
   *
   **/
  private def deQUntilSpan = {
    logger.debug(s"(latest -> earliest): ($latestTime -> $earliestTime)\t" +
      s"(spanInQueue vs requiredSpan)): ($spanInQueue vs $spanInSeconds)")
    while (spanInQueue > spanInSeconds) deQ()
  }

  private def deQ() = {
    logger.debug("pop")
    val poped = queue.dequeue()

    cumulativeValue -= poped.bag.eventCount
    if (poped.alertToggle equals Up) {
      alertingElementsInQueue -= 1
    }
    poped
  }


  private def updateIfAlert(element: BaseAggMetrics) = {
    val newAvg = averageValue
    if (newAvg >= threshold) {
      logger.debug(s"alerting element - (t: threshold, cv: cumulativeValue, siq: spanInQueue, elem: element)" +
        s" (t: $threshold, cv: $cumulativeValue, tsn: $spanInQueue, elem: $element)")
      queue.last.alertToggle = Up
      alertingElementsInQueue += 1
    }
  }

  private def verifyNonEmpty = if (queue.isEmpty) throw new IndexOutOfBoundsException("The Alert Queue is currently empty")

}

object AlertQueue {

  sealed trait AlertToggle {}

  case object Up extends AlertToggle

  case object Down extends AlertToggle

  case class Element(bag: BaseAggMetrics, var alertToggle: AlertToggle)

}
