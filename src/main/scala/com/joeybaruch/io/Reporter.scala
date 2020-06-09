package com.joeybaruch.io

import com.joeybaruch.alerts.AlertQueue
import com.joeybaruch.datamodel.Observer
import com.joeybaruch.metrics.MetricCollector

trait Reporter{
  val metricReporter: MetricsReporter
  val alertReporter: AlertReporter
}

trait MetricsReporter extends Observer[MetricCollector]

trait AlertReporter extends Observer[AlertQueue]


