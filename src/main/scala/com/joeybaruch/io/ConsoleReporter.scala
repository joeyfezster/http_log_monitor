package com.joeybaruch.io
import com.joeybaruch.alerts.AlertQueue
import com.joeybaruch.alerts.AlertQueue.{Down, Up}
import com.joeybaruch.io.ConsoleReporter.{ConsoleAlertReporter, ConsoleMetricsReporter}
import com.joeybaruch.metrics.MetricCollector

class ConsoleReporter extends Reporter {
  override val metricReporter: MetricsReporter = ConsoleMetricsReporter
  override val alertReporter: AlertReporter = ConsoleAlertReporter
}

object ConsoleReporter {
  case object ConsoleMetricsReporter extends MetricsReporter {
    override def receiveUpdate(subject: MetricCollector): Unit = {
      println (subject.currentMetrics)
    }
  }

  case object ConsoleAlertReporter extends AlertReporter{
    override def receiveUpdate(subject: AlertQueue): Unit = {
      subject.alertStatus match {
        case Up => println(s"High traffic generated an alert - hits = ${subject.averageValue}, triggered at ${subject.lastStatusChangeTime}")
        case Down => println(s"High traffic alert recovered at ${subject.lastStatusChangeTime}")
      }
    }
  }
}
