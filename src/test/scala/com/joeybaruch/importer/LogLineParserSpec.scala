package com.joeybaruch.importer

import java.sql.Timestamp

import com.joeybaruch.datamodel.{Headers, LogEvent, Request, UnparsableEvent}
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class LogLineParserSpec extends AnyFlatSpec with Matchers {

  behavior of "LogLineParser"
  private val config: Config = ConfigFactory.load()
  val logLineParser = new LogLineParser(config)

  it should "parse valid log lines" in {
    val parsedEventWithSection = logLineParser.parse(goodLineWithSection)
    val parsedEventWithoutSection = logLineParser.parse(goodLineWithoutSection)

    parsedEventWithSection should equal (goodLineEventWithSection)
    parsedEventWithoutSection should equal (goodLineEventWithoutSection)
  }

  it should "parse a headers line" in {
    val headers = config.getString("schema.legal-headers")
    val parsedHeaders = logLineParser.parse(headers)

    parsedHeaders should be (Headers(headers))
  }

  it should "parse a bad line as Unparsable" in {
    val parsedBadLine = logLineParser.parse(badLine)
    parsedBadLine should be (UnparsableEvent(badLine))
  }

  it should "parse a good line with a bad request as Unparsable" in {
    val parsedGoodLineWithBadRequest = logLineParser.parse(goodLineWithBadRequest)
    parsedGoodLineWithBadRequest should be (UnparsableEvent(goodLineWithBadRequest))
  }

  //todo: whitebox test the regex

  private val host = "10.0.0.2"
  private val rfc = "-"
  private val authUser = "apache"
  private val strTimestamp = "1549573860"
  private val protocol = "HTTP/1.0"
  private val method = "GET"
  private val section = "user"
  private val endpointWithSection = "/api/" + section
  private val endpointWithoutSection = "/report"
  private val strRequestWithSection = method + " " + endpointWithSection + " " + protocol
  private val strRequestWithoutSection = method + " " + endpointWithoutSection + " " + protocol
  private val strBadRequest = "bad request here - too many fields"
  private val bytes = "1234"
  private val status = "200"

  val goodLineWithSection = "\"" + host + "\",\"" + rfc + "\",\"" + authUser + "\"," + strTimestamp + ",\"" +
    strRequestWithSection + "\"," + status + "," + bytes
  val goodLineWithoutSection = "\"" + host + "\",\"" + rfc + "\",\"" + authUser + "\"," + strTimestamp + ",\"" +
    strRequestWithoutSection + "\"," + status + "," + bytes
  val goodLineWithBadRequest = "\"" + host + "\",\"" + rfc + "\",\"" + authUser + "\"," + strTimestamp + ",\"" +
    strBadRequest + "\"," + status + "," + bytes
  val badLine = "fdjkdfjkd, jkdjkfdl, jk"

  val requestWithSection = Request(method, endpointWithSection, Some(section), protocol)
  val requestWithoutSection = Request(method, endpointWithoutSection, None, protocol)

  val goodLineEventWithSection = LogEvent(host, rfc, authUser, new Timestamp(strTimestamp.toLong), requestWithSection, status.toInt, bytes.toInt)
  val goodLineEventWithoutSection = LogEvent(host, rfc, authUser, new Timestamp(strTimestamp.toLong), requestWithoutSection, status.toInt, bytes.toInt)
}
