package com.joeybaruch.alerts

import akka.Done
import akka.stream.scaladsl.{Flow, Keep, Sink}
import com.joeybaruch.datamodel.AggregatedMetrics.BaseAggMetrics
import com.joeybaruch.windowing.Aggregators._

import scala.concurrent.Future

object AlertSink {
  def alertingSink(observedAlertQueue: ObservedAlertQueue): Sink[BaseAggMetrics, Future[Done]] = {
    Flow[BaseAggMetrics]
      .map(bag => {
        assert(isOneSecond(bag))
        bag
      })
      .statefulMapConcat { () =>
        element => {
          observedAlertQueue.enQ(element)
          Seq()
        }
      }.toMat(Sink.ignore)(Keep.right)
  }

}
