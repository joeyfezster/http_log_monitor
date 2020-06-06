package com.joeybaruch.monitor

import com.joeybaruch.datamodel.{Headers, LogEvent, Request, UnparsableEvent}
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ColumnarLogParserSpec extends AnyFlatSpec with Matchers with BeforeAndAfter {

  var config: Config = _
  var parserUnderTest: ColumnarLogParser = _

  before {
    config = ConfigFactory.load()
    parserUnderTest = new ColumnarLogParser(config)
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

  private lazy val host = "10.0.0.2"
  private lazy val rfc = "-"
  private lazy val authUser = "apache"
  private lazy val strTimestamp = "1549573860"
  private lazy val protocol = "HTTP/1.0"
  private lazy val method = "GET"
  private lazy val section = "user"
  private lazy val endpointWithSection = "/api/" + section
  private lazy val endpointWithoutSection = "/report"
  private lazy val strRequestWithSection = method + " " + endpointWithSection + " " + protocol
  private lazy val strRequestWithoutSection = method + " " + endpointWithoutSection + " " + protocol
  private lazy val strBadRequest = "bad request here - too many fields"
  private lazy val bytes = "1234"
  private lazy val status = "200"

  lazy val goodLineWithSection = List(host, rfc, authUser, strTimestamp, strRequestWithSection, status, bytes)
  lazy val goodLineWithoutSection = List(host, rfc, authUser, strTimestamp, strRequestWithoutSection, status, bytes)
  lazy val goodLineWithBadRequest = List(host, rfc, authUser, strTimestamp, strBadRequest, status, bytes)
  lazy val badLine = List("fdjkdfjkd, jkdjkfdl, jk", "jkjfdsa", "jkfdasda")

  lazy val requestWithSection: Request = Request(method, endpointWithSection, Some(section), protocol)
  lazy val requestWithoutSection: Request = Request(method, endpointWithoutSection, None, protocol)

  lazy val goodLineEventWithSection: LogEvent = LogEvent(host, rfc, authUser, strTimestamp.toLong, requestWithSection, status.toInt, bytes.toInt)
  lazy val goodLineEventWithoutSection: LogEvent = LogEvent(host, rfc, authUser, strTimestamp.toLong, requestWithoutSection, status.toInt, bytes.toInt)
}
