package com.joeybaruch.alerts

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.joeybaruch.windowing.EventsWindow

object AlertFlows {
  def processAlerts(observedAlertQueue: ObservedAlertQueue): Flow[EventsWindow, Nothing, NotUsed] = {
    Flow[EventsWindow]
      .map(win => {
        assert(win.isSingleTimeUnit)
        win
      })
      .statefulMapConcat { () =>
        element => {
          observedAlertQueue.enQueue(element)
          Seq()
        }
      }
  }
}
