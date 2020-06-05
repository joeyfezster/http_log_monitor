package com.joeybaruch.monitor

import com.joeybaruch.datamodel.LogLine

trait LogParser {
  def parse(columns: List[String], filename: Option[String] = None): LogLine
}

