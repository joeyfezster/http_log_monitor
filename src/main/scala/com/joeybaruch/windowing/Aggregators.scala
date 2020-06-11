package com.joeybaruch.windowing

import akka.NotUsed
import akka.stream.contrib.AccumulateWhileUnchanged
import akka.stream.scaladsl.Flow
import com.joeybaruch.datamodel.LegalLogEvent.LogEvent
import com.joeybaruch.metrics.AggregatedMetrics
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration._

object Aggregators extends LazyLogging {

  def oneSecondAggregator: Flow[LogEvent, AggregatedMetrics, NotUsed] = {
    import AggregatedMetrics._ //implicit conversions (.as[SomethingElseThanOriginalType])

    Flow[LogEvent]
      .via(AccumulateWhileUnchanged[LogEvent, Long](le => le.timestamp))
      .map(seq => seq.map(_.as[AggregatedMetrics]))
      .map(seq => AggregatedMetrics.aggregate(seq))
      .map(transparentlyAssertOneSecond)
  }


  def aggregatedMetricsTumblingWindow(config: Config): Flow[AggregatedMetrics, AggregatedMetrics, NotUsed] = {
    Flow[AggregatedMetrics]
      .map(transparentlyAssertOneSecond)
      .statefulMapConcat(() => {
        val state = new AggregatingWindowState(config)

        event => {
          state.aggregate(event)
        }
      })
  }

  private class AggregatingWindowState(config: Config) {
    val maxWindowSize: Long = config.getInt("metrics.report-every.seconds").seconds.toSeconds
    var agg: AggregatedMetrics = AggregatedMetrics.emptyAggregatedMetrics

    def aggregate(event: AggregatedMetrics): Seq[AggregatedMetrics] = {
      val tentative = event.eventsWindow + agg.eventsWindow
      if (tentative.timeSpan <= maxWindowSize) {
        agg = agg + event
        Seq()
      }
      else {
        val emmit = agg
        agg = event
        Seq(emmit)
      }
    }
  }

  def isOneSecond(metrics: AggregatedMetrics): Boolean = {
    metrics.eventsWindow.isSingleTimeUnit
  }

  private def transparentlyAssertOneSecond(aggregatedMetrics: AggregatedMetrics) = {
    assert(isOneSecond(aggregatedMetrics))
    aggregatedMetrics
  }


}
