package com.joeybaruch.metrics

import akka.Done
import akka.stream.scaladsl.{Flow, Keep, Sink}
import com.joeybaruch.datamodel.AggregatedMetrics

import scala.concurrent.Future

object MetricsSink {
  def metricsSink(observedMetricsCollector: ObservedMetricsCollector): Sink[AggregatedMetrics, Future[Done]] = {
    Flow[AggregatedMetrics]
      .map(observedMetricsCollector.collect)
      .toMat(Sink.ignore)(Keep.right)
  }

}
