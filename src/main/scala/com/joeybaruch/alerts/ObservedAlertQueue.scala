package com.joeybaruch.alerts

import com.joeybaruch.alerts.AlertQueue.{Down, Up}
import com.joeybaruch.datamodel.{AggregatedMetrics, ObservedSubject}
import com.typesafe.config.Config

class ObservedAlertQueue(config: Config) extends AlertQueue(config) with ObservedSubject[AlertQueue] {
  override def enQ(element: AggregatedMetrics.BaseAggMetrics): Unit = {
    if(this.isEmpty) {
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
