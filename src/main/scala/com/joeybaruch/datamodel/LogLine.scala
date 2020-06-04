package com.joeybaruch.datamodel

import java.sql.Timestamp

sealed trait LogLine {}

case class Headers(headers: String) extends LogLine
case class Request(method: String, endpoint: String, section: Option[String] = None, protocol: String)
case class LogEvent(host: String,
                    rfc931: String,
                    authUser: String,
                    timestamp: Timestamp,
                    request: Request,
                    status: Int,
                    bytes: Int) extends LogLine
case class UnparsableEvent(string: String) extends LogLine

