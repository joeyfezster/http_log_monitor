package com.joeybaruch.monitor

import com.joeybaruch.datamodel._
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging

class CSVLogParser(config: Config) extends LogParser with LazyLogging {

  def parse(columns: List[String], filename: Option[String] = None): LogLine = try {
    logger.debug(s"parsing $columns")

    import scala.jdk.CollectionConverters._
    val headers = config.getList("schema.legal-headers").asScala.map(_.unwrapped().toString).toList

    columns match {
      case _  if columns == headers =>
        logger.info(s"Headers match in file $filename")
        Headers(headers)
      case _ => parseEventLine(columns)
    }
  } catch {
    case e: Throwable =>
      logger.error(s"Failed to parse from ${filename.getOrElse("")}\n $columns: $e: ${e.getMessage}")
      UnparsableEvent(columns)
  }


  private def parseEventLine(columns: List[String]): LogLine = try {
    columns match {
      //todo: is there a better way to enforce schema?
      case List(remoteHost, rfc931, authUser, timestamp, request, status, bytes)
        if areNumeric(timestamp, status, bytes) =>
        LogEvent(remoteHost, rfc931, authUser, timestamp.toLong, getRequest(request), status.toInt, bytes.toInt)
      case _ => logger.error(s"unable to parse log event: $columns"); UnparsableEvent(columns)
    }
  }

  private def getRequest(request: String): Request = {
    request.split(" ") match {
      case Array(method, endpoint, protocol) => Request(method, endpoint, getSection(endpoint), protocol)
      case _ => lazy val msg = s"request does not align to <method endpoint protocol>: $request"
        logger.error(msg)
        throw new IllegalArgumentException(msg)
    }
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
