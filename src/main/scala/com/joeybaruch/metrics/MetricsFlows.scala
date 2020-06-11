package com.joeybaruch.metrics

import akka.NotUsed
import akka.stream.scaladsl.Flow

object MetricsFlows {
  def collectMetrics(observedMetricsCollector: ObservedMetricsCollector): Flow[AggregatedMetrics, AggregatedMetrics, NotUsed] = {
    Flow[AggregatedMetrics]
      .map(observedMetricsCollector.collect)
  }

}
