package com.joeybaruch.metrics

import akka.Done
import akka.stream.scaladsl.{Flow, Keep, Sink}
import com.joeybaruch.datamodel.AggregatedMetrics.AggMetrics

import scala.concurrent.Future

object MetricsSink {
  def metricsSink(observedMetricsCollector: ObservedMetricsCollector): Sink[AggMetrics, Future[Done]] = {
    Flow[AggMetrics]
      .map(observedMetricsCollector.collect)
      .toMat(Sink.ignore)(Keep.right)
  }

}
