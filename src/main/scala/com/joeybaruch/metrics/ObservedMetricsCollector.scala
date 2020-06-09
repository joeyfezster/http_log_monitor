package com.joeybaruch.metrics

import com.joeybaruch.datamodel.{AggregatedMetrics, ObservedSubject}

class ObservedMetricsCollector  extends MetricCollector with ObservedSubject[MetricCollector]{
  override def collect(AggregatedMetrics: AggregatedMetrics): AggregatedMetrics = {
    val res = super.collect(AggregatedMetrics)
    notifyObservers()
    res
  }
}
