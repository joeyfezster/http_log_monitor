package com.joeybaruch.windowing

import akka.stream.contrib.AccumulateWhileUnchanged
import akka.stream.scaladsl.{Flow, Keep, Sink}
import akka.{Done, NotUsed}
import com.joeybaruch.datamodel.AggregatedMetrics.BaseAggMetrics
import com.joeybaruch.datamodel.{AggregatedMetrics, LogEvent}

import scala.concurrent.Future

object GenericAggregator {
  val timeSpan = 10L

//  def oneSecondAggregator[T](transform: LogEvent => T, aggregate: Seq[T] => T): Flow[LogEvent, T, NotUsed] =
//    Flow[LogEvent]
//      .via(AccumulateWhileUnchanged(le => le.timestamp))
//      .map(logEventSequence => logEventSequence.map(transform))
//      .map(aggregate)


  def oneSecondAggregator: Flow[LogEvent, BaseAggMetrics, NotUsed] = {
    import AggregatedMetrics._

    Flow[LogEvent]
      .via(AccumulateWhileUnchanged[LogEvent, Long](le => le.timestamp))
      .map { case seq => seq.map(_.as[BaseAggMetrics]) }
      .map { case seq => AggregatedMetrics.aggregate(seq) }
    //assert 1 second
  }

  def alertingSink(alert: BaseAggMetrics => Unit): Sink[BaseAggMetrics, Future[Done]] = {
    Flow[BaseAggMetrics] //assumes that each BAG represents one second worth of events
      .statefulMapConcat { () =>
        val state = new MapConcatState(alert)
        val methods = new MapConcatMethods(state)

        element => {
          methods.enQ(element)
        }
      }.toMat(Sink.ignore)(Keep.right)
  }
  //  def box(transform: LogEvent => ) :Sink[BaseAggMetrics,Future[Done]] =
  //    oneSecondAggregator(transform, aggregate)

  final class MapConcatState(alert: BaseAggMetrics => Unit){
    val queue = ???
  }

  final private class MapConcatMethods(state: MapConcatState){
    def enQ(element: BaseAggMetrics) = ???

    private def deQ = ???

    private def mustAlert: Boolean = ???

    private def mustRecover: Boolean = ???


  }

}
