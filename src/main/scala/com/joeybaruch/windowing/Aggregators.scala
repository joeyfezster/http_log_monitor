package com.joeybaruch.windowing

import akka.NotUsed
import akka.stream.contrib.AccumulateWhileUnchanged
import akka.stream.scaladsl.Flow
import com.joeybaruch.datamodel.AggregatedMetrics.AggMetrics
import com.joeybaruch.datamodel.{AggregatedMetrics, LogEvent}
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration._

object Aggregators extends LazyLogging {

  def oneSecondAggregator: Flow[LogEvent, AggMetrics, NotUsed] = {
    import AggregatedMetrics._

    Flow[LogEvent]
      .via(AccumulateWhileUnchanged[LogEvent, Long](le => le.timestamp))
      .map(seq => seq.map(_.as[AggMetrics]))
      .map(seq => AggregatedMetrics.aggregate(seq))
      .map(transparentlyAssertOneSecond)
  }


  def aggregatedMetricsTumblingWindow(config: Config): Flow[AggMetrics, AggMetrics, NotUsed] = {
    Flow[AggMetrics]
      .map(transparentlyAssertOneSecond)
      .statefulMapConcat(() => {
        val state = new AggregatingWindowState(config)

        event => {
          state.aggregate(event)
        }
      })
  }

  final class AggregatingWindowState(config: Config) {
    val maxWindowSize: Long = config.getInt("windowing.metrics-window-size.seconds").seconds.toSeconds
    var agg: AggMetrics = AggregatedMetrics.emptyAggMetrics

    def aggregate(event: AggMetrics): Seq[AggMetrics] = {
      val tentative = event.baseAggregatedMetrics + agg.baseAggregatedMetrics
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

  def isOneSecond(dag: AggMetrics): Boolean = {
    val bag = dag.baseAggregatedMetrics
    isOneSecond(bag)
  }

  def isOneSecond(bag: AggregatedMetrics.BaseAggMetrics): Boolean = {
    bag.earliestTimestamp == bag.latestTimestamp
  }

  private def transparentlyAssertOneSecond(dag: AggMetrics) = {
    assert(isOneSecond(dag))
    dag
  }


}
