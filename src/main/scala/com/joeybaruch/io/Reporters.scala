package com.joeybaruch.io

import com.joeybaruch.alerts.AlertQueue
import com.joeybaruch.datamodel.Observer
import com.joeybaruch.metrics.MetricCollector

object Reporters {

  trait MetricsReporter extends Observer[MetricCollector]

  trait AlertReporter extends Observer[AlertQueue]

}


