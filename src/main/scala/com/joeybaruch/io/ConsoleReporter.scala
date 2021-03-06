package com.joeybaruch.io

import com.joeybaruch.alerts.AlertQueue
import com.joeybaruch.alerts.AlertQueue.{Down, Up}
import com.joeybaruch.io.Reporters.{AlertReporter, MetricsReporter}
import com.joeybaruch.metrics.MetricCollector

object ConsoleReporter {

  case object ConsoleMetricsReporter extends MetricsReporter {
    override def receiveUpdate(subject: MetricCollector): Unit = {
      println(subject.currentMetrics)
      Console.flush()
    }
  }

  case object ConsoleAlertReporter extends AlertReporter {
    override def receiveUpdate(subject: AlertQueue): Unit = {
      subject.alertStatus match {
        case Up =>
          println(f"High traffic generated an alert - hits = ${subject.averageValue}%.2f, " +
            s"triggered at ${subject.lastStatusChangeTime}")
          Console.flush()
        case Down =>
          println(s"High traffic alert recovered at ${subject.lastStatusChangeTime}")
          Console.flush()
      }
    }
  }

}
