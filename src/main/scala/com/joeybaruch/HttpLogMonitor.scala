package com.joeybaruch

import java.io.File

import akka.actor.ActorSystem
import akka.stream.ClosedShape
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, RunnableGraph, Sink, Source}
import akka.{Done, NotUsed}
import com.joeybaruch.alerts.{AlertFlows, ObservedAlertQueue}
import com.joeybaruch.io.ConsoleReporter
import com.joeybaruch.metrics.{AggregatedMetrics, MetricsFlows, ObservedMetricsCollector}
import com.joeybaruch.parser.{ColumnarLogParser, FileDataReader}
import com.joeybaruch.windowing.{Aggregators, EventFlowAligner}
import com.typesafe.config.Config

import scala.concurrent.Future

object HttpLogMonitor {

  def runForFile(file: File, config: Config)(implicit system: ActorSystem): (Future[Done], Future[Done]) = {

    /** Objects for Akka Graph **/
    val parser = new ColumnarLogParser(config)
    val fileDataReader = new FileDataReader(parser)

    val observedAlertQueue = new ObservedAlertQueue(config)
    val observedMetricsCollector = new ObservedMetricsCollector
    val consoleReporter = ConsoleReporter

    // connect reporters to observed
    observedAlertQueue.addObserver(consoleReporter.ConsoleAlertReporter)
    observedMetricsCollector.addObserver(consoleReporter.ConsoleMetricsReporter)

    /** Graph Stages  **/
    val aggregatedMetricSource: Source[AggregatedMetrics, NotUsed] = fileDataReader.fileSource(file)
      .via(EventFlowAligner.timeAligned(config))
      .via(Aggregators.oneSecondAggregator)

    val metricsSink: Sink[AggregatedMetrics, Future[Done]] =
      Flow[AggregatedMetrics]
        .via(Aggregators.aggregatedMetricsTumblingWindow(config))
        .via(MetricsFlows.collectMetrics(observedMetricsCollector))
        .toMat(Sink.ignore)(Keep.right)

    val alertSink: Sink[AggregatedMetrics, Future[Done]] =
      Flow[AggregatedMetrics]
        .map(_.getEventsWindow)
        .via(AlertFlows.processAlerts(observedAlertQueue))
        .toMat(Sink.ignore)(Keep.right)

    /** Broadcast - split the stream to alerts and metrics separately **/
    val (alerts: Future[Done], metrics: Future[Done]) =
      RunnableGraph
        .fromGraph(GraphDSL.create(alertSink, metricsSink)(Tuple2.apply) {
          implicit builder =>
            (alertsS, metricsS) =>
              import GraphDSL.Implicits._
              val broadcast = builder.add(Broadcast[AggregatedMetrics](2))
              aggregatedMetricSource ~> broadcast
              broadcast ~> alertsS
              broadcast ~> metricsS
              ClosedShape
        }).run()

    (alerts, metrics)
  }
}
