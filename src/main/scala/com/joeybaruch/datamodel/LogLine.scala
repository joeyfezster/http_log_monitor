package com.joeybaruch.datamodel

import com.joeybaruch.datamodel.AggregatedMetrics.DebugAggregatedMetrics

sealed trait LogLine {}

case class Headers(headers: List[String]) extends LogLine

case class Request(method: String, endpoint: String, section: Option[String] = None, protocol: String)

case class LogEvent(host: String,
                    rfc931: String,
                    authUser: String,
                    timestamp: Long,
                    request: Request,
                    status: String,
                    bytes: Int) extends LogLine with Ordered[LogEvent] {

  // negating the compareTo result of two positive numbers will result in ascending order - which we need for oldest first
  override def compare(that: LogEvent): Int = -this.timestamp.compareTo(that.timestamp)

  def as[T](implicit f: LogEvent => T): T = f(this)
}

object LogEvent {
  implicit def mapToDebugAggregatedMetric(event: LogEvent): DebugAggregatedMetrics = {
    val oneStatus = Map(event.status -> 1L)
    val oneHttpMethod = Map(event.request.method -> 1L)
    val oneHttpMethodWithStatus = Map(event.request.method -> oneStatus)
    val oneHost = Map(event.host -> 1L)
    val oneHostWithStatus = Map(event.host -> oneStatus)
    val oneSection = event.request.section.fold(Map.empty[String, Long])(section => Map(section -> 1L))
    val oneUser = Map(event.authUser -> 1L)
    val oneSectionWithStatus = event.request.section.fold(Map.empty[String, Map[String, Long]])(section => Map(section -> oneStatus))
    val oneUserWithStatus = Map(event.authUser -> oneStatus)

    new DebugAggregatedMetrics(1L, event.timestamp, event.timestamp,
      oneHttpMethod, oneHttpMethodWithStatus,
      oneHost, oneHostWithStatus,
      oneSection, oneSectionWithStatus,
      oneUser, oneUserWithStatus,
      oneStatus, event.bytes)
  }
}

case class UnparsableEvent(string: Seq[String]) extends LogLine
