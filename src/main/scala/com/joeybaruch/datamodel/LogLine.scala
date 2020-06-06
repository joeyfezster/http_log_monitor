package com.joeybaruch.datamodel

sealed trait LogLine {}

case class Headers(headers: List[String]) extends LogLine

case class Request(method: String, endpoint: String, section: Option[String] = None, protocol: String)

case class LogEvent(host: String,
                    rfc931: String,
                    authUser: String,
                    timestamp: Long,
                    request: Request,
                    status: Int,
                    bytes: Int) extends LogLine with Ordered[LogEvent]{

  // negating the compareTo result of two positive numbers will result in ascending order - which we need for oldest first
  override def compare(that: LogEvent): Int = - this.timestamp.compareTo(that.timestamp)
}

case class UnparsableEvent(string: Seq[String]) extends LogLine
