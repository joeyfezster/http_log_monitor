package com.joeybaruch.metrics

import com.joeybaruch.datamodel.AggregatedMetrics
import com.typesafe.scalalogging.LazyLogging

class MetricCollector extends LazyLogging {
  var currentMetrics: AggregatedMetrics = _

  def collect(AggregatedMetrics: AggregatedMetrics): AggregatedMetrics = {
    currentMetrics = AggregatedMetrics
    AggregatedMetrics
  }
}
