package com.joeybaruch.datamodel

import com.joeybaruch.datamodel.AggregatedMetrics.BaseAggregatedMetrics
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class AggregatedMetricsSpec extends AnyFlatSpec with Matchers {

  import AggregatedMetrics._

  behavior of "TruncatedAggregatedMetrics"
  it should "aggregate correctly" in {
    truncated1 + truncated2 should equal(truncated1Plus2)
  }

  val truncated1 = new BaseAggregatedMetrics(1, 1L, 1L, Map[String, Long]("s1" -> 1L))
  val truncated2 = new BaseAggregatedMetrics(1, 2L, 2L, Map[String, Long]("s1" -> 1L))
  val truncated1Plus2 = new BaseAggregatedMetrics(2, 1L, 2L, Map[String, Long]("s1" -> 2L))

}
