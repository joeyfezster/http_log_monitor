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
                    bytes: Int) extends LogLine

case class UnparsableEvent(string: Seq[String]) extends LogLine

