package com.joeybaruch.windowing

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.Flow
import com.joeybaruch.datamodel.LogEvent.SentinelEOFEvent
import com.joeybaruch.datamodel.{LogEvent, LogEventImpl}
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging

import scala.collection.mutable
import scala.concurrent.duration._

class EventFlowAligner(config: Config)(implicit system: ActorSystem) extends LazyLogging {

  //todo: move to object so I don't have to instantiate the class - what about configs?
  val timeAligned: Flow[LogEvent, LogEvent, NotUsed] = {
    Flow[LogEvent]
      .statefulMapConcat { () =>
        val state = new MapConcatState
        val methods = new QueueMethods(state)

        event => {
          event match {
            case SentinelEOFEvent => methods.flushQueue()
            case _: LogEventImpl =>
              methods.enqueueEvent(event)
              methods.dequeWatermarkedEvents(Seq())
          }
        }
      }
  }

  final class MapConcatState {
    val allowedDelay: Long = config.getInt("windowing.late-data.delay-allowed.seconds").seconds.toSeconds
    var latestSeenTimestamp = 0L
    val oldestEventFirstQueue: mutable.PriorityQueue[LogEvent] = mutable.PriorityQueue.empty[LogEvent]

    logger.debug(s"allowed delay set to $allowedDelay")
  }

  final class QueueMethods(state: MapConcatState) {
    def isOlderThanWatermark(event: LogEvent): Boolean = {
      val watermark = state.latestSeenTimestamp - state.allowedDelay
      event.timestamp < watermark
    }

    def enqueueEvent(event: LogEvent): Unit = {
      if (isOlderThanWatermark(event)) logger.info(s"a log event arrived after allowed delay $event")
      else {
        logger.debug(s"enqueuing event $event")
        if (state.latestSeenTimestamp < event.timestamp) {
          logger.debug(s"updated latest seen from ${state.latestSeenTimestamp} to ${event.timestamp}")
          state.latestSeenTimestamp = event.timestamp
        }
        state.oldestEventFirstQueue.addOne(event)
      }
    }

    @scala.annotation.tailrec
    def dequeWatermarkedEvents(agg: Seq[LogEvent]): Seq[LogEvent] = {
      state.oldestEventFirstQueue.headOption match {
        // events in the queue that are "older than watermark" are no longer waiting for potential events to "cut the line"
        case Some(event) if isOlderThanWatermark(event) => dequeWatermarkedEvents(agg :+ state.oldestEventFirstQueue.dequeue())
        case _ => agg match {
          case Seq() => logger.debug(s"no elements to de-queue")
          case Seq(_, _*) => logger.debug(s"de-queuing sequence of events: $agg")
        }
          agg
      }
    }

    def flushQueue(): Seq[LogEvent] = state.oldestEventFirstQueue.dequeueAll
  }

}

