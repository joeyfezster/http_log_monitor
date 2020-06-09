package com.joeybaruch.metrics

import com.joeybaruch.datamodel.{AggregatedMetrics, ObservedSubject}

class ObservedMetricsCollector  extends MetricCollector with ObservedSubject[MetricCollector]{
  override def collect(aggMetrics: AggregatedMetrics.AggMetrics): AggregatedMetrics.AggMetrics = {
    val res = super.collect(aggMetrics)
    notifyObservers()
    res
  }
}
