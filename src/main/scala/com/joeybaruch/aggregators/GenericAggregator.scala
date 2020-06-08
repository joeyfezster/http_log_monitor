package com.joeybaruch.aggregators

import akka.NotUsed
import akka.stream.contrib.AccumulateWhileUnchanged
import akka.stream.scaladsl.Flow
import com.joeybaruch.datamodel.LogEvent

object GenericAggregator {
  val timeSpan = 10L

  def aggregator[T](transform: LogEvent => T, aggregate: Seq[T] => T): Flow[LogEvent, T, NotUsed] =
    Flow[LogEvent]
    .via(AccumulateWhileUnchanged(le => le.timestamp))
    .map(logEventSequence => logEventSequence.map(transform))
    .map(aggregate)
}


