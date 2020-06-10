package com.joeybaruch.alerts

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.joeybaruch.windowing.Aggregators._
import com.joeybaruch.windowing.EventsWindow

object AlertFlows {
  def processAlerts(observedAlertQueue: ObservedAlertQueue): Flow[EventsWindow, Nothing, NotUsed] = {
    Flow[EventsWindow]
      .map(win => {
        assert(isOneSecond(win))
        win
      })
      .statefulMapConcat { () =>
        element => {
          observedAlertQueue.enQ(element)
          Seq()
        }
      }
  }

}
