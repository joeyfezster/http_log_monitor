package com.joeybaruch

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import com.joeybaruch.alerts.{AlertSink, ObservedAlertQueue}
import com.joeybaruch.datamodel.AggregatedMetrics
import com.joeybaruch.io.ConsoleReporter
import com.joeybaruch.metrics.{MetricsSink, ObservedMetricsCollector}
import com.joeybaruch.parser.{ColumnarLogParser, FileDataReader}
import com.joeybaruch.windowing.{Aggregators, EventFlowAligner}
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration._

//Eli: in general it looks ok (I don't understand each bit of it, but don't have time to go over it).
//Eli: but notice: You are using mutable state (like an internal queue etc.) inside graph stages. It might lead to problems. With akka streams everything should be immutable. I don't think you have time to fix it now, but it's just for your reference.
object HttpLogMonitor {

  def runForFile(file: String) = {
    implicit val system: ActorSystem = ActorSystem()

    val config = ConfigFactory.load()
    val parser = new ColumnarLogParser(config)

    val fileDataReader = new FileDataReader(config, parser)

    //Eli: you are materializing the source twice (each run does it), meaning you will go over the file TWICE! This is against what they wrote in the instructions.
    val AggregatedMetricsource: Source[AggregatedMetrics, NotUsed] = fileDataReader.fileSource(file)
      .via(EventFlowAligner.timeAligned(config))
      .via(Aggregators.oneSecondAggregator)

    val observedAlertQueue = new ObservedAlertQueue(config)
    val observedMetricsCollector = new ObservedMetricsCollector
    val consoleReporter = ConsoleReporter
    observedAlertQueue.addObserver(consoleReporter.ConsoleAlertReporter)
    observedMetricsCollector.addObserver(consoleReporter.ConsoleMetricsReporter)

    val alertSinkFuture = AggregatedMetricsource
      .map(_.getEventsWindow)
      .runWith(AlertSink.alertingSink(observedAlertQueue))
    val metricsSinkFuture = AggregatedMetricsource
      .via(Aggregators.aggregatedMetricsTumblingWindow(config))
      .runWith(MetricsSink.metricsSink(observedMetricsCollector))

    //Eli: NEVER use Await.result for a future. In your case it will crash with an exception after 3 seconds
    Await.result(alertSinkFuture, 3.seconds)
    //Eli: For both results you are not "watching" the result (and in case someone will watch it from outside, it will be only for the second Future
    Await.result(metricsSinkFuture, 3.seconds)

  }
}
