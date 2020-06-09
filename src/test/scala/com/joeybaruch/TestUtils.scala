package com.joeybaruch

import java.nio.file.Paths

import com.joeybaruch.datamodel.AggregatedMetrics.BaseAggMetrics
import com.joeybaruch.datamodel.{LogEvent, LogEventImpl, Request}

object TestUtils {
  def bag(ts: Long, ec: Long): BaseAggMetrics = {
    BaseAggMetrics(ec, ts, ts)
  }

  val smallSampleFile: String = Paths.get(getClass.getResource("/sample/small_sample_csv.txt").toURI).toString
  val smallSampleWithSomeBadLogs: String = Paths.get(getClass.getResource("/sample/small_sample_bad_csv.txt").toURI).toString
  val logEvent1: LogEvent = LogEventImpl("10.0.0.2", "-", "apache", 1549573860, Request("get", "/api/user", Some("/api"), "http/1.0"), "200", 1234)
  val logEvent2: LogEvent = LogEventImpl("10.0.0.4", "-", "apache", 1549573861, Request("get", "/api/user", Some("/api"), "http/1.0"), "200", 1234)
  val logEvent3: LogEvent = LogEventImpl("10.0.0.4", "-", "apache", 1549573862, Request("get", "/api/user", Some("/api"), "http/1.0"), "200", 1234)
  val logEvent4: LogEvent = LogEventImpl("10.0.0.4", "-", "apache", 1549573863, Request("get", "/api/user", Some("/api"), "http/1.0"), "200", 1234)
  val logEvent5: LogEvent = LogEventImpl("10.0.0.4", "-", "apache", 1549573864, Request("get", "/api/user", Some("/api"), "http/1.0"), "200", 1234)

}
