package com.joeybaruch.monitor

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.alpakka.csv.scaladsl.CsvParsing
import akka.stream.scaladsl.{Sink, Source}
import akka.util.ByteString
import com.joeybaruch.datamodel.{LogEvent, Request}
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfter, GivenWhenThen}

import scala.concurrent.Await
import scala.concurrent.duration._

class FileDataReaderSpec extends AnyFlatSpec with Matchers with GivenWhenThen with BeforeAndAfter with LazyLogging {

  implicit val system = ActorSystem()

  var config: Config = _
  var logParser: LogParser = _
  var fileDataReader: FileDataReader = _

  before {
    config = ConfigFactory.load()
    logParser = new CSVLogParser(config)
    fileDataReader = new FileDataReader(config, logParser)
  }

  behavior of "processSingleFile"
  it should "return a single file list when using the hack" in {
    //    val methodUnderTest = fileDataReader.get
  }

  behavior of "processFile"
  it should "parse a file with log events" in {
    Given("An Akka Source that reads a file and outputs a stream of log events")
    val sourceUnderTest = new FileDataReader(config, new CSVLogParser(config)).processFile(smallSampleFile)

    When("it's run into a sink")
    val future = sourceUnderTest.take(10).runWith(Sink.seq)
    val result = Await.result(future, 3.seconds)

    Then("We should expect a sequence of the legal log events")
    assert(result == Seq(logEvent1, logEvent2))
  }


  behavior of "FileDataReader import flow"

  it should "parse a csv file to the model data case classes" in {

  }

  val smallSampleFile = Paths.get(getClass.getResource("/sample/small_sample_csv.txt").toURI).toString
  val logEvent1 = LogEvent("10.0.0.2", "-", "apache", 1549573860, Request("GET", "/api/user", Some("user"), "HTTP/1.0"), 200, 1234)
  val logEvent2 = LogEvent("10.0.0.4", "-", "apache", 1549573860, Request("GET", "/api/user", Some("user"), "HTTP/1.0"), 200, 1234)
}
