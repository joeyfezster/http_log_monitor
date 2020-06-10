package com.joeybaruch

import akka.actor.ActorSystem
import akka.stream.ClosedShape
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, RunnableGraph, Sink, Source}
import akka.{Done, NotUsed}
import com.joeybaruch.alerts.{AlertFlows, ObservedAlertQueue}
import com.joeybaruch.io.ConsoleReporter
import com.joeybaruch.metrics.{AggregatedMetrics, MetricsFlows, ObservedMetricsCollector}
import com.joeybaruch.parser.{ColumnarLogParser, FileDataReader}
import com.joeybaruch.windowing.{Aggregators, EventFlowAligner}
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.Future

//Eli: in general it looks ok (I don't understand each bit of it, but don't have time to go over it).
//Eli: but notice: You are using mutable state (like an internal queue etc.) inside graph stages. It might lead to problems. With akka streams everything should be immutable. I don't think you have time to fix it now, but it's just for your reference.
object HttpLogMonitor {

  def runForFile(file: String, config: Config) = {
    implicit val system: ActorSystem = ActorSystem()

    val parser = new ColumnarLogParser(config)

    val fileDataReader = new FileDataReader(parser)

    //Eli: you are materializing the source twice (each run does it), meaning you will go over the file TWICE! This is against what they wrote in the instructions.
    //Joey: does the broadcast below fix that?
    val aggregatedMetricSource: Source[AggregatedMetrics, NotUsed] = fileDataReader.fileSource(file)
      .via(EventFlowAligner.timeAligned(config))
      .via(Aggregators.oneSecondAggregator)

    val observedAlertQueue = new ObservedAlertQueue(config)
    val observedMetricsCollector = new ObservedMetricsCollector
    val consoleReporter = ConsoleReporter
    observedAlertQueue.addObserver(consoleReporter.ConsoleAlertReporter)
    observedMetricsCollector.addObserver(consoleReporter.ConsoleMetricsReporter)

    val metricsSinkFuture: Sink[AggregatedMetrics, Future[Done]] =
      Flow[AggregatedMetrics]
        .via(Aggregators.aggregatedMetricsTumblingWindow(config))
        .via(MetricsFlows.collectMetrics(observedMetricsCollector))
        .toMat(Sink.ignore)(Keep.right)


    val alertSink: Sink[AggregatedMetrics, Future[Done]] =
      Flow[AggregatedMetrics]
        .map(_.getEventsWindow)
        .via(AlertFlows.processAlerts(observedAlertQueue))
        .toMat(Sink.ignore)(Keep.right)


    val (alerts: Future[Done], metrics: Future[Done]) =
      RunnableGraph
        .fromGraph(GraphDSL.create(alertSink, metricsSinkFuture)(Tuple2.apply) {
          implicit builder =>
            (alertsS, metricsS) =>
              import GraphDSL.Implicits._
              val broadcast = builder.add(Broadcast[AggregatedMetrics](2))
              aggregatedMetricSource ~> broadcast
              broadcast.out(0) ~> alertsS
              broadcast.out(1) ~> metricsS
              ClosedShape
        })
        .run()

    //    //    val balancer = GraphDSL.create() { implicit builder =>
    //    //      import GraphDSL.Implicits._
    //    //
    //    //      val balance = builder.add(Broadcast[AggregatedMetrics](2))
    //    //
    //    //      balance ~> alertSink
    //    //      balance ~> metricsSinkFuture
    //    //
    //    //      SinkShape(balance.in)
    //    //    }
    //
    //    //Eli: NEVER use Await.result for a future. In your case it will crash with an exception after 3 seconds
    //    Await.result(alertSink, 3.seconds)
    //    //Eli: For both results you are not "watching" the result (and in case someone will watch it from outside, it will be only for the second Future
    //    //    Await.result(metricsSinkFuture, 3.seconds)

  }
}
