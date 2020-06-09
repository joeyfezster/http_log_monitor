package com.joeybaruch.windowing

import com.joeybaruch.datamodel.{LogEvent, WindowedEventsMonoid}

import scala.math.{max, min}

case class EventsWindow(eventCount: Long,
                        earliestTimestamp: Long,
                        latestTimestamp: Long) {

  val timeSpan: Long = latestTimestamp - earliestTimestamp + 1


  def +(that: EventsWindow): EventsWindow = {
    val newEventCount = this.eventCount + that.eventCount
    val newEarliestTimestamp = min(this.earliestTimestamp, that.earliestTimestamp)
    val newLatestTimestamp = max(this.latestTimestamp, that.latestTimestamp)

    EventsWindow(newEventCount, newEarliestTimestamp, newLatestTimestamp)
  }
}

object EventsWindow {
  val emptyEventsWindow: EventsWindow = EventsWindow(0L, Long.MaxValue, Long.MinValue)

  implicit val eventsWindowMonoid: WindowedEventsMonoid[EventsWindow] =
    new WindowedEventsMonoid[EventsWindow] {
      override def empty: EventsWindow = emptyEventsWindow

      override def combine(win1: EventsWindow, win2: EventsWindow): EventsWindow = win1 + win2
    }


  implicit def logEventToEventsWindow(event: LogEvent): EventsWindow = {
    EventsWindow(1L, event.timestamp, event.timestamp)
  }
}
