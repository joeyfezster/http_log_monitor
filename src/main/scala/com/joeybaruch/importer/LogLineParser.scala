package com.joeybaruch.importer

import java.sql.Timestamp

import com.joeybaruch.datamodel._
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging

class LogLineParser(config: Config) extends LazyLogging {

  def parse(line: String): LogLine = try {
    line match {
      case _ if line == config.getString("schema.legal-headers") => Headers(line)
      case _ => parseEventLine(line)
    }
  } catch
    {
      case e: Throwable =>logger.error(s"Failed to parse line $line\n; $e: ${e.getMessage}"); UnparsableEvent(line)
    }


  private def parseEventLine(line: String): LogLine = try {
    line.split(",").map(_.trim).map(removeQuotes) match {
      //todo: is there a better way to enforce schema?
      case Array(remoteHost, rfc931, authUser, timestamp, request, status, bytes)
        if areNumeric(timestamp, status, bytes) =>
        LogEvent(remoteHost, rfc931, authUser, new Timestamp(timestamp.toLong), getRequest(request), status.toInt, bytes.toInt)
      case _ => logger.error(s"unable to parse log event: $line"); UnparsableEvent(line)
    }
  }

  private def getRequest(request: String): Request = {
    request.split(" ") match {
      case Array(method, endpoint, protocol) => Request(method, endpoint, getSection(endpoint), protocol)
      case _ => lazy val msg = s"request does not align to <method endpoint protocol>: $request"
        logger.error(msg);
        throw new IllegalArgumentException(msg)
    }
  }

  private def removeQuotes(string: String): String = {
    val firstAndLastQuoteRegex = "^\"|\"$"
    string.replaceAll(firstAndLastQuoteRegex, "")
  }

  private def areNumeric(inputs: String*): Boolean = inputs.forall(str => str.forall(char => char.isDigit))

  private def getSection(endpoint: String): Option[String] = {
    val components = endpoint.split("/")
    val sectionComponentIndex = config.getInt("schema.section-component-index")

    if (sectionComponentIndex < components.length) {
      logger.debug(s"extracted section ${components(sectionComponentIndex)} from $endpoint")
      Some(components(sectionComponentIndex))
    }
    else None
  }
}
