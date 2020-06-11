package com.joeybaruch.datamodel

trait LogLine {}

case class Headers(headers: List[String]) extends LogLine

case class Request(method: String, endpoint: String, section: Option[String] = None, protocol: String)

case class UnparsableEvent(string: Seq[String]) extends LogLine
