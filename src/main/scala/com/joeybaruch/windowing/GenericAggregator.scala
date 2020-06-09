package com.joeybaruch.windowing

import akka.stream.contrib.AccumulateWhileUnchanged
import akka.stream.scaladsl.{Flow, Keep, Sink}
import akka.{Done, NotUsed}
import com.joeybaruch.alerts.ObservedAlertQueue
import com.joeybaruch.datamodel.AggregatedMetrics.AggMetrics
import com.joeybaruch.datamodel.{AggregatedMetrics, LogEvent}

import scala.concurrent.Future

object GenericAggregator {

  def oneSecondAggregator: Flow[LogEvent, AggMetrics, NotUsed] = {
    import AggregatedMetrics._

    def isOneSecond(dag: AggMetrics) = {
      dag.baseAggregatedMetrics.earliestTimestamp == dag.baseAggregatedMetrics.latestTimestamp
    }

    Flow[LogEvent]
      .via(AccumulateWhileUnchanged[LogEvent, Long](le => le.timestamp))
      .map(seq => seq.map(_.as[AggMetrics]))
      .map(seq => AggregatedMetrics.aggregate(seq))
      .map(dag => {
        assert(isOneSecond(dag))
        dag
      })
  }

  def alertingSink(observedAlertQueue: ObservedAlertQueue): Sink[AggMetrics, Future[Done]] = {
    Flow[AggMetrics] //assumes that each BAG represents one second worth of events
      .map(dag => dag.truncate)
      .statefulMapConcat { () =>
        element => {
          observedAlertQueue.enQ(element)
          Seq()
        }
      }.toMat(Sink.ignore)(Keep.right)
  }


}
