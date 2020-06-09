package com.joeybaruch.datamodel

import com.joeybaruch.datamodel.AggregatedMetrics.{BaseAggMetrics, AggMetrics}

sealed trait LogLine {}

case class Headers(headers: List[String]) extends LogLine

case class Request(method: String, endpoint: String, section: Option[String] = None, protocol: String)


sealed trait LogEvent extends LogLine with Ordered[LogEvent] {
  val host: String
  val rfc931: String
  val authUser: String
  val timestamp: Long
  val request: Request
  val status: String
  val bytes: Int

  def as[T](implicit f: LogEvent => T): T = f(this)

  // negating the compareTo result of two positive numbers will result in ascending order - which we need for oldest first
  override def compare(that: LogEvent): Int = -this.timestamp.compareTo(that.timestamp)

}


case class LogEventImpl(host: String,
                        rfc931: String,
                        authUser: String,
                        timestamp: Long,
                        request: Request,
                        status: String,
                        bytes: Int) extends LogEvent

object LogEvent {
  //todo: test this
  implicit def mapToAggMetric(event: LogEvent): AggMetrics = {
    val oneStatus = Map(event.status -> 1L)
    val oneHttpMethod = Map(event.request.method -> 1L)
    val oneHttpMethodWithStatus = Map(event.request.method -> oneStatus)
    val oneHost = Map(event.host -> 1L)
    val oneHostWithStatus = Map(event.host -> oneStatus)
    val oneSection = event.request.section.fold(Map.empty[String, Long])(section => Map(section -> 1L))
    val oneUser = Map(event.authUser -> 1L)
    val oneSectionWithStatus = event.request.section.fold(Map.empty[String, Map[String, Long]])(section => Map(section -> oneStatus))
    val oneUserWithStatus = Map(event.authUser -> oneStatus)

    val baseAggMetrics = BaseAggMetrics(1L, event.timestamp, event.timestamp)

    AggMetrics(baseAggMetrics,
      oneHttpMethod, oneHttpMethodWithStatus,
      oneHost, oneHostWithStatus,
      oneSection, oneSectionWithStatus,
      oneUser, oneUserWithStatus,
      oneStatus, event.bytes)
  }

    implicit def mapToBaseAggregatedMetric(event: LogEvent): BaseAggMetrics = {
      mapToAggMetric(event).truncate
    }


  case object SentinelEOFEvent extends LogEvent {
    //todo: review this paradigm - pattern matching vs implementing unnecessary functions
    private val emptyStr = ""
    private val emptyReq = Request(emptyStr, emptyStr, None, emptyStr)
    override val host: String = emptyStr
    override val rfc931: String = emptyStr
    override val authUser: String = emptyStr
    override val timestamp: Long = 0L
    override val request: Request = emptyReq
    override val status: String = emptyStr
    override val bytes: Int = 0
  }

}

case class UnparsableEvent(string: Seq[String]) extends LogLine
