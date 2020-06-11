package com.joeybaruch

import java.io.File

import com.joeybaruch.alerts.AlertQueue
import com.joeybaruch.alerts.AlertQueue.{Down, Up}
import com.joeybaruch.datamodel.LegalLogEvent.{LogEvent, LogEventImpl}
import com.joeybaruch.datamodel.{Observer, Request}
import com.joeybaruch.windowing.EventsWindow

object TestUtils {
  def oneSecWin(ts: Long, ec: Long): EventsWindow = {
    EventsWindow(ec, ts, ts)
  }

  def copyEventWithNewTimestamp(event: LogEvent, timestamp: Long): LogEventImpl = {
    LogEventImpl(event.host, event.rfc931, event.authUser, timestamp, event.request, event.status, event.bytes)
  }

  // todo: this should be mocked
  case class TestAlertReporter(var alertIsTriggered: Boolean = false, var alertIsRecovered: Boolean = false)
    extends Observer[AlertQueue] {
    def receiveUpdate(alertQueue: AlertQueue): Unit = {
      alertQueue.alertStatus match {
        case Up => alertIsTriggered = true
        case Down => alertIsRecovered = true
      }
    }

    def reset(): Unit = {
      alertIsTriggered = false
      alertIsRecovered = false
    }

    def status: (Boolean, Boolean) = (alertIsTriggered, alertIsRecovered)
  }

  val smallSampleFile: File = new File(getClass.getResource("/sample/small_sample_csv.txt").toURI)
  val smallSampleWithSomeBadLogs: File = new File(getClass.getResource("/sample/small_sample_bad_csv.txt").toURI)
  val fullSampleFile: File = new File(getClass.getResource("/sample/sample_csv.txt").toURI)

  val logEvent1: LogEvent = LogEventImpl("10.0.0.2", "-", "apache", 1549573860, Request("get", "/api/user", Some("/api"), "http/1.0"), "200", 1234)
  val logEvent2: LogEvent = LogEventImpl("10.0.0.4", "-", "apache", 1549573861, Request("get", "/api/user", Some("/api"), "http/1.0"), "200", 1234)
  val logEvent3: LogEvent = LogEventImpl("10.0.0.4", "-", "apache", 1549573862, Request("get", "/api/user", Some("/api"), "http/1.0"), "200", 1234)
  val logEvent4: LogEvent = LogEventImpl("10.0.0.4", "-", "apache", 1549573863, Request("get", "/api/user", Some("/api"), "http/1.0"), "200", 1234)
  val logEvent5: LogEvent = LogEventImpl("10.0.0.4", "-", "apache", 1549573864, Request("get", "/api/user", Some("/api"), "http/1.0"), "200", 1234)

  //10 first events from file
  LogEventImpl("10.0.0.2", "-", "apache", 1549573860, Request("get", "/api/user", Some("/api"), "http/1.0"), "200", 1234)
  LogEventImpl("10.0.0.4", "-", "apache", 1549573860, Request("get", "/api/user", Some("/api"), "http/1.0"), "200", 1234)
  LogEventImpl("10.0.0.4", "-", "apache", 1549573860, Request("get", "/api/user", Some("/api"), "http/1.0"), "200", 1234)
  LogEventImpl("10.0.0.2", "-", "apache", 1549573860, Request("get", "/api/help", Some("/api"), "http/1.0"), "200", 1234)
  LogEventImpl("10.0.0.5", "-", "apache", 1549573860, Request("get", "/api/help", Some("/api"), "http/1.0"), "200", 1234)
  LogEventImpl("10.0.0.4", "-", "apache", 1549573859, Request("get", "/api/help", Some("/api"), "http/1.0"), "200", 1234)
  LogEventImpl("10.0.0.4", "-", "apache", 1549573861, Request("get", "/api/user", Some("/api"), "http/1.0"), "200", 1136)
  LogEventImpl("10.0.0.3", "-", "apache", 1549573860, Request("get", "/report", Some("/report"), "http/1.0"), "200", 1194)
  LogEventImpl("10.0.0.3", "-", "apache", 1549573860, Request("post", "/report", Some("/report"), "http/1.0"), "200", 1234)
  LogEventImpl("10.0.0.5", "-", "apache", 1549573860, Request("post", "/report", Some("/report"), "http/1.0"), "500", 1307)
}
