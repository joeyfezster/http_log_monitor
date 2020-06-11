package com.joeybaruch.windowing

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import com.joeybaruch.TestUtils._
import com.joeybaruch.metrics.AggregatedMetrics
import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.Await
import scala.concurrent.duration._

class AggregatorsSpec extends AnyFlatSpec with Matchers with BeforeAndAfter {

  implicit val system: ActorSystem = ActorSystem()

  var config: Config = _

  before {
    config = ConfigFactory.load("test-conf")
      .withValue("metrics.report-every.seconds", ConfigValueFactory.fromAnyRef(3))
  }

  behavior of "one second aggregator"
  it should "transform an ordered stream of log events with different times each" in {
    val orderedSequenceOfLogEventsOnDifferentSeconds = Seq(logEvent1, logEvent2, logEvent3, logEvent4, logEvent5)
    val expectedSequenceOfAggregatedMetrics = orderedSequenceOfLogEventsOnDifferentSeconds.map(_.as[AggregatedMetrics])
    val flowUnderTest = Aggregators.oneSecondAggregator

    val future = Source(orderedSequenceOfLogEventsOnDifferentSeconds).via(flowUnderTest).runWith(Sink.seq)
    val result = Await.result(future, 3.seconds)
    result should contain theSameElementsInOrderAs expectedSequenceOfAggregatedMetrics
  }

  it should "combine an ordered steam of log events with overlapping timestamps by timestamp" in {
    val time1 = 1L
    val time2 = 2L
    val time3 = 3L
    val le1 = copyEventWithNewTimestamp(logEvent1, time1)
    val le2 = copyEventWithNewTimestamp(logEvent1, time1)
    val le3 = copyEventWithNewTimestamp(logEvent1, time2)
    val le4 = copyEventWithNewTimestamp(logEvent1, time2)
    val le5 = copyEventWithNewTimestamp(logEvent1, time2)
    val le6 = copyEventWithNewTimestamp(logEvent1, time3)
    val sequenceOf5LogEventsOver2ConsecutiveSeconds = Seq(le1, le2, le3, le4, le5, le6)

    val expectedSequenceOfAggregatedMetrics = Seq(le1.as[AggregatedMetrics] + le2.as[AggregatedMetrics],
      le3.as[AggregatedMetrics] + le4.as[AggregatedMetrics] + le5.as[AggregatedMetrics],
      le6.as[AggregatedMetrics])

    val flowUnderTest = Aggregators.oneSecondAggregator

    val future = Source(sequenceOf5LogEventsOver2ConsecutiveSeconds).via(flowUnderTest).runWith(Sink.seq)
    val result = Await.result(future, 3.seconds)
    result should contain theSameElementsInOrderAs expectedSequenceOfAggregatedMetrics

  }

  behavior of "metrics aggregator"
  it should "aggregate log event representations to windows by window size" in {
    val aggM1 = copyEventWithNewTimestamp(logEvent1, 1L).as[AggregatedMetrics]
    val aggM2 = copyEventWithNewTimestamp(logEvent2, 2L).as[AggregatedMetrics]
    val aggM3 = copyEventWithNewTimestamp(logEvent3, 3L).as[AggregatedMetrics]
    val aggM4 = copyEventWithNewTimestamp(logEvent4, 4L).as[AggregatedMetrics]
    val aggM5 = copyEventWithNewTimestamp(logEvent5, 5L).as[AggregatedMetrics]
    val trigger = copyEventWithNewTimestamp(logEvent5, 50L).as[AggregatedMetrics]

    val aggSeq = Seq(aggM1, aggM2, aggM3, aggM4, aggM5, trigger)

    val flowUnderTest = Aggregators.aggregatedMetricsTumblingWindow(config)
    val future = Source(aggSeq).via(flowUnderTest).runWith(Sink.seq)

    val result = Await.result(future, 3.seconds)
    result should contain theSameElementsInOrderAs Seq(aggM1 + aggM2 + aggM3, aggM4 + aggM5)
  }
}
