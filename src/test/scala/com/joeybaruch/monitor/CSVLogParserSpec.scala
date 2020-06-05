package com.joeybaruch.monitor

import com.joeybaruch.datamodel.{Headers, LogEvent, Request, UnparsableEvent}
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class CSVLogParserSpec extends AnyFlatSpec with Matchers with BeforeAndAfter {

  var config: Config = _
  var parserUnderTest: CSVLogParser = _

  before {
    config = ConfigFactory.load()
    parserUnderTest = new CSVLogParser(config)
  }

  behavior of "parse"
  it should "parse a correct event with a section in the request" in {
    val result = parserUnderTest.parse(goodLineWithSection)
    result should be(goodLineEventWithSection)
  }

  it should "parse a correct event without a section in the request" in {
    val result = parserUnderTest.parse(goodLineWithoutSection)
    result should be(goodLineEventWithoutSection)
  }

  it should "parse an event with a bad request as unparsable" in {
    val result = parserUnderTest.parse(goodLineWithBadRequest)
    result should be(UnparsableEvent(goodLineWithBadRequest))
  }

  it should "parse headers" in {
    import scala.jdk.CollectionConverters._
    val headers = config.getList("schema.legal-headers").asScala.map(_.unwrapped().toString).toList
    val result = parserUnderTest.parse(headers)

    result should be(Headers(headers))
  }

  it should "parse a bad line as unparsable" in {
    val result = parserUnderTest.parse(badLine)
    result should be(UnparsableEvent(badLine))
  }

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

  val goodLineWithSection = List(host, rfc, authUser, strTimestamp, strRequestWithSection, status, bytes)
  val goodLineWithoutSection = List(host, rfc, authUser, strTimestamp, strRequestWithoutSection, status, bytes)
  val goodLineWithBadRequest = List(host, rfc, authUser, strTimestamp, strBadRequest, status, bytes)
  val badLine = List("fdjkdfjkd, jkdjkfdl, jk", "jkjfdsa", "jkfdasda")

  val requestWithSection = Request(method, endpointWithSection, Some(section), protocol)
  val requestWithoutSection = Request(method, endpointWithoutSection, None, protocol)

  val goodLineEventWithSection = LogEvent(host, rfc, authUser, strTimestamp.toLong, requestWithSection, status.toInt, bytes.toInt)
  val goodLineEventWithoutSection = LogEvent(host, rfc, authUser, strTimestamp.toLong, requestWithoutSection, status.toInt, bytes.toInt)
}
