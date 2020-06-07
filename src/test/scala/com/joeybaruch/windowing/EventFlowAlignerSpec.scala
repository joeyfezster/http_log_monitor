package com.joeybaruch.windowing

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import com.joeybaruch.datamodel.{LogEvent, Request}
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.Await
import scala.concurrent.duration._

class EventFlowAlignerSpec extends AnyFlatSpec with Matchers with BeforeAndAfter {

  implicit val system: ActorSystem = ActorSystem()
  var config: Config = _
  var eventFlowAligner: EventFlowAligner = _
  var allowedDelay: Long = _
  var minUnalowedDelay: Long = _

  before {
    config = ConfigFactory.load()
    eventFlowAligner = new EventFlowAligner(config)
    allowedDelay = config.getInt("windowing.late-data.delay-allowed.seconds").seconds.toSeconds
    minUnalowedDelay = allowedDelay + 1
  }

  behavior of "timeAligned"
  it should "output events in order" in {
    val unOrderedInputData = addTriggeringEvent(unorderedEventSequenceWith3SecMaxLateness)
    val orderedExpectedOutput = unorderedEventSequenceWith3SecMaxLateness.sortBy(_.timestamp)

    val flowUnderTest = eventFlowAligner.timeAligned
    val future = Source(unOrderedInputData).via(flowUnderTest).runWith(Sink.seq)
    val result = Await.result(future, 3.seconds)

    result should contain theSameElementsInOrderAs orderedExpectedOutput
  }

  it should "discard late data" in {
    var eventSequenceWithLateData = orderedEventSequence :+ addOffsetToEvent(orderedEventSequence.last, minUnalowedDelay)
    eventSequenceWithLateData = addTriggeringEvent(eventSequenceWithLateData)

    val flowUnderTest = eventFlowAligner.timeAligned
    val future = Source(eventSequenceWithLateData).via(flowUnderTest).runWith(Sink.seq)

    val result = Await.result(future, 3.seconds)
    result should contain theSameElementsInOrderAs orderedEventSequence
  }

  lazy val logEvent1: LogEvent = LogEvent("10.0.0.2", "-", "apache", 1549573860, Request("GET", "/api/user", Some("user"), "HTTP/1.0"), "200", 1234)
  lazy val logEvent2: LogEvent = LogEvent("10.0.0.4", "-", "apache", 1549573861, Request("GET", "/api/user", Some("user"), "HTTP/1.0"), "200", 1234)
  lazy val logEvent3: LogEvent = LogEvent("10.0.0.4", "-", "apache", 1549573862, Request("GET", "/api/user", Some("user"), "HTTP/1.0"), "200", 1234)
  lazy val logEvent4: LogEvent = LogEvent("10.0.0.4", "-", "apache", 1549573863, Request("GET", "/api/user", Some("user"), "HTTP/1.0"), "200", 1234)
  lazy val latestEvent: LogEvent = LogEvent("10.0.0.4", "-", "apache", 1549573864, Request("GET", "/api/user", Some("user"), "HTTP/1.0"), "200", 1234)

  def addOffsetToEvent(event: LogEvent, offset: Long) = {
    val timesamp = event.timestamp - offset
    getEventWithTimestamp(event, timesamp)
  }

  private def getEventWithTimestamp(event: LogEvent, timesamp: Long) = {
    LogEvent(event.host, event.rfc931, event.authUser, timesamp, event.request, event.status, event.bytes)
  }

  def addTriggeringEvent(seq: Seq[LogEvent]): Seq[LogEvent] = {
    val latestEvent = seq.maxBy(_.timestamp)
    seq :+ addOffsetToEvent(latestEvent, -minUnalowedDelay)
  }

  lazy val orderedEventSequence = Seq(logEvent1, logEvent2, logEvent3, logEvent4, latestEvent)
  lazy val unorderedEventSequenceWith3SecMaxLateness = Seq(logEvent3, logEvent1, logEvent4, latestEvent, logEvent2)

}
