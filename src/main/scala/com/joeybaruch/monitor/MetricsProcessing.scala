package com.joeybaruch.monitor

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.Flow
import com.joeybaruch.datamodel.AggregatedMetrics.DebugAggregatedMetrics
import com.joeybaruch.datamodel.LogEvent
import com.typesafe.scalalogging.LazyLogging

class MetricsProcessing()(implicit system: ActorSystem) extends LazyLogging {
  val oneSecondGranularity: Flow[LogEvent, DebugAggregatedMetrics, NotUsed] = {
    Flow[LogEvent]
      .map{case event: LogEvent => event.as[DebugAggregatedMetrics]}
  }
}

