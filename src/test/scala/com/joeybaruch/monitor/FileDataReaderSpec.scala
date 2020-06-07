package com.joeybaruch.monitor

import java.nio.file.Paths

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import com.joeybaruch.datamodel.LogEvent.SentinelEOFEvent
import com.joeybaruch.datamodel.{LogEvent, LogEventImpl, Request}
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration._

class FileDataReaderSpec extends AnyFlatSpec with Matchers with BeforeAndAfter with LazyLogging {

  implicit val system: ActorSystem = ActorSystem()

  var config: Config = _
  var logParser: LogParser = _
  var fileDataReader: FileDataReader = _

  before {
    config = ConfigFactory.load()
    logParser = new ColumnarLogParser(config)
    fileDataReader = new FileDataReader(config, logParser)
  }

  behavior of "processFile"
  it should "parse a file with log events" in {
    val result = runParseForFile(smallSampleFile)

    result should contain theSameElementsInOrderAs  fileParseResults
  }


  it should "parse a file with some bad log events" in {
    val result = runParseForFile(smallSampleWithSomeBadLogs)

    result should contain theSameElementsInOrderAs  fileParseResults
  }

  behavior of "Implicit Ordering of LogEvents"
  it should "testing min q" in {
    val q = mutable.PriorityQueue.empty[LogEvent]
    val logs = Seq(logEvent2, logEvent1)

    q.addAll(logs)

    val result = q.dequeueAll
    result should contain theSameElementsInOrderAs logs.sortBy(_.timestamp)
  }


  private def runParseForFile(filename: String): Seq[LogEvent] = {
    val sourceUnderTest = new FileDataReader(config, new ColumnarLogParser(config)).processFile(filename)

    val future = sourceUnderTest.take(10).runWith(Sink.seq)
    val result = Await.result(future, 3.seconds)
    result
  }

  lazy val smallSampleFile: String = Paths.get(getClass.getResource("/sample/small_sample_csv.txt").toURI).toString
  lazy val smallSampleWithSomeBadLogs: String = Paths.get(getClass.getResource("/sample/small_sample_bad_csv.txt").toURI).toString
  lazy val logEvent1: LogEvent = LogEventImpl("10.0.0.2", "-", "apache", 1549573860, Request("get", "/api/user", Some("/api"), "http/1.0"), "200", 1234)
  lazy val logEvent2: LogEvent = LogEventImpl("10.0.0.4", "-", "apache", 1549573861, Request("get", "/api/user", Some("/api"), "http/1.0"), "200", 1234)
  private val fileParseResults = Seq(logEvent1, logEvent2, SentinelEOFEvent)
}
