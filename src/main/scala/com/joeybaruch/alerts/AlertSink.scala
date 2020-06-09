package com.joeybaruch.alerts

import akka.Done
import akka.stream.scaladsl.{Flow, Keep, Sink}
import com.joeybaruch.windowing.Aggregators._
import com.joeybaruch.windowing.EventsWindow

import scala.concurrent.Future

object AlertSink {
  def alertingSink(observedAlertQueue: ObservedAlertQueue): Sink[EventsWindow, Future[Done]] = {
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
      }.toMat(Sink.ignore)(Keep.right)
  }

}
