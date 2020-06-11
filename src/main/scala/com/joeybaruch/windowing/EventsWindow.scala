package com.joeybaruch.windowing

import com.joeybaruch.datamodel.LegalLogEvent.LogEvent
import com.joeybaruch.datamodel.{LegalLogEvent, WindowedEventsMonoid}

import scala.math.{max, min}

case class EventsWindow(eventCount: Long,
                        winStartTime: Long,
                        winEndTime: Long) {

  val timeSpan: Long = winEndTime - winStartTime + 1

  def +(that: EventsWindow): EventsWindow = {
    val newEventCount = this.eventCount + that.eventCount
    val newEarliestTimestamp = min(this.winStartTime, that.winStartTime)
    val newLatestTimestamp = max(this.winEndTime, that.winEndTime)

    EventsWindow(newEventCount, newEarliestTimestamp, newLatestTimestamp)
  }

  def isSingleTimeUnit: Boolean = this.winEndTime == this.winEndTime
}

object EventsWindow {

  val emptyEventsWindow: EventsWindow = EventsWindow(0L, Long.MaxValue, Long.MinValue)

  /** *************     Implicit monoid and conversions        **************/
  import scala.language.implicitConversions

  implicit val eventsWindowMonoid: WindowedEventsMonoid[EventsWindow] =
    new WindowedEventsMonoid[EventsWindow] {
      override def empty: EventsWindow = emptyEventsWindow

      override def combine(win1: EventsWindow, win2: EventsWindow): EventsWindow = win1 + win2
    }


  implicit def logEventToEventsWindow(event: LogEvent): EventsWindow = {
    EventsWindow(1L, event.timestamp, event.timestamp)
  }
}
