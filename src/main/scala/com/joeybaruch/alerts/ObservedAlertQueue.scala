package com.joeybaruch.alerts

import com.joeybaruch.alerts.AlertQueue.{Down, Up}
import com.joeybaruch.datamodel.ObservedSubject
import com.joeybaruch.windowing.EventsWindow
import com.typesafe.config.Config

class ObservedAlertQueue(config: Config) extends AlertQueue(config) with ObservedSubject[AlertQueue] {
  override def enQ(element: EventsWindow): Unit = {
    if (this.isEmpty) {
      super.enQ(element)
      return
    }

    val prev = alertStatus
    super.enQ(element)
    val current = alertStatus

    // only notify when there is a change from the previous state to the new state
    (prev, current) match {
      case (Up, Down) => notifyObservers()
      case (Down, Up) => notifyObservers()
      case _ => () //do nothing
    }
  }
}
