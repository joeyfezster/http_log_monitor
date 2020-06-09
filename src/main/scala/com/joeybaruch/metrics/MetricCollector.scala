package com.joeybaruch.metrics

import com.joeybaruch.datamodel.AggregatedMetrics.AggMetrics
import com.typesafe.scalalogging.LazyLogging

class MetricCollector extends LazyLogging {
  var currentMetrics: AggMetrics = _

  def collect(aggMetrics: AggMetrics): AggMetrics = {
    currentMetrics = aggMetrics
    aggMetrics
  }
}
