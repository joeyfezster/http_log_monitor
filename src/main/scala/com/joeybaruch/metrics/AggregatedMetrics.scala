package com.joeybaruch.metrics

import com.joeybaruch.datamodel.{LogEvent, WindowedEventsMonoid}
import com.joeybaruch.metrics.AggregatedMetrics._
import com.joeybaruch.windowing.EventsWindow
import com.typesafe.config.ConfigFactory

import scala.util.Try

case class AggregatedMetrics(eventsWindow: EventsWindow,

                             /** ****** business metrics *********/
                             httpMethodsCounters: NamedCountersCollection,
                             // could specific http methods/verbs be having negative trends?
                             httpMethodResponseStatusCounters: NamedCountersHyperCollection,

                             hostsCounters: NamedCountersCollection,
                             // could specific hosts have bad response trends?
                             hostResponseStatusCounters: NamedCountersHyperCollection,

                             sectionCounters: NamedCountersCollection, // required by product !!
                             // could specific sections have bad response trends?
                             sectionResponseStatusCounters: NamedCountersHyperCollection,

                             usersCounters: NamedCountersCollection,
                             // could specific users have bad response trends?
                             userResponseStatusCounter: NamedCountersHyperCollection,

                             statusCounters: NamedCountersCollection,
                             bytesCounter: Long
                            ) {
  def +(that: AggregatedMetrics): AggregatedMetrics = {
    val newEventsWindow = this.eventsWindow + that.eventsWindow

    val newHttpMethodsCounters = this.httpMethodsCounters unionWith that.httpMethodsCounters
    val newHttpMethodResponseStatusCounters = this.httpMethodResponseStatusCounters unionWith that.httpMethodResponseStatusCounters

    val newHostsCounters = this.hostsCounters unionWith that.hostsCounters
    val newHostResponseStatusCounters = this.hostResponseStatusCounters unionWith that.hostResponseStatusCounters

    val newSectionCounters = this.sectionCounters unionWith that.sectionCounters
    val newSectionResponseStatusCounters = this.sectionResponseStatusCounters unionWith that.sectionResponseStatusCounters

    val newUsersCounters = this.usersCounters unionWith that.usersCounters
    val newUserResponseStatusCounters = this.userResponseStatusCounter unionWith that.userResponseStatusCounter

    val newStatusCounters = this.statusCounters unionWith that.statusCounters
    val newBytesCounter = this.bytesCounter + that.bytesCounter

    AggregatedMetrics(newEventsWindow,
      newHttpMethodsCounters, newHttpMethodResponseStatusCounters,
      newHostsCounters, newHostResponseStatusCounters,
      newSectionCounters, newSectionResponseStatusCounters,
      newUsersCounters, newUserResponseStatusCounters,
      newStatusCounters, newBytesCounter)
  }

  def getEventsWindow: EventsWindow = this.eventsWindow


  def debugStats: String = {
    val span = eventsWindow.timeSpan
    s"    ------  Debug Stats   ----------" +
      f"\n\tTotal bytes for period: $bytesCounter\t ${bytesCounter.toDouble / span}%.2f/sec" +
      s"\n\tCalls by return status: ${printNCC(statusCounters, span)}" +
      s"\n\tSection calls by response status: ${printNCHC(sectionResponseStatusCounters, span)} " +
      s"\n\tCalls by Http method: ${printNCC(httpMethodsCounters, span)}" +
      s"\n\tCalls by Http method by response status: ${printNCHC(httpMethodResponseStatusCounters, span)} " +
      s"\n\tCalls by user: ${printNCC(usersCounters, span)}" +
      s"\n\tCalls by user by response status: ${printNCHC(userResponseStatusCounter, span)} " +
      s"\n\tCalls by hosts: ${printNCC(hostsCounters, span)}" +
      s"\n\tCalls by hosts by response status: ${printNCHC(hostResponseStatusCounters, span)} "
  }


  private val default3: Int = 3
  // the following are unaffected by configs set at the test level, change in .conf file or as environment variable
  private val showDebugStats: Boolean = Try(ConfigFactory.load().getBoolean("show-debug-stats")).getOrElse(true)
  private val topN: Int = Try(ConfigFactory.load().getInt("top-sections-to-show")).getOrElse(default3)


  def topSections: String = {
    val descendingSortedSectionsByCount = sectionCounters.toSeq.sortWith(_._2 > _._2)
    val topNSections = descendingSortedSectionsByCount.take(topN)
    topNSections.foldLeft("") {
      case (agg, (name, value)) =>
        agg.concat(s"\n\t\tsection $name -> with $value calls")
    }
  }

  override def toString: String = {
    val str = s"Metrics for period - ${eventsWindow.earliestTimestamp} : ${eventsWindow.latestTimestamp} (${eventsWindow.timeSpan} seconds):\n" +
      s"    ------  Quick Stats   ----------" +
      s"\n\tTotal legal log lines: ${eventsWindow.eventCount}" +
      s"\n\tTop Sections Hit: $topSections" +
      s"\n\tCall rate: ${eventsWindow.eventCount.toDouble / eventsWindow.timeSpan} calls per second" +
      s"\n"

    (if (showDebugStats) str.concat(debugStats) else str) concat "\n\\-----------------------------------/"
  }
}


object AggregatedMetrics {

  def aggregate[T: WindowedEventsMonoid](seq: Seq[T]): T = implicitly[WindowedEventsMonoid[T]].aggregate(seq)


  /** *************     Implicit monoids for our Aggregated Metrics containers        **************/

  implicit val AggregatedMetricsMonoid: WindowedEventsMonoid[AggregatedMetrics] =
    new WindowedEventsMonoid[AggregatedMetrics] {
      override def empty: AggregatedMetrics = emptyAggregatedMetrics

      override def combine(AggregatedMetrics1: AggregatedMetrics, AggregatedMetrics2: AggregatedMetrics): AggregatedMetrics =
        AggregatedMetrics1 + AggregatedMetrics2
    }

  type NamedCountersCollection = Map[String, Long]
  type NamedCountersHyperCollection = Map[String, NamedCountersCollection]

  /** *************     Empty Units for Monoids        **************/
  private def emptyImmutableNCC: NamedCountersCollection = Map.empty[String, Long]

  private def emptyImmutableNCHC: NamedCountersHyperCollection = Map.empty[String, NamedCountersCollection]


  val emptyAggregatedMetrics: AggregatedMetrics = AggregatedMetrics(EventsWindow.emptyEventsWindow,
    emptyImmutableNCC, emptyImmutableNCHC,
    emptyImmutableNCC, emptyImmutableNCHC,
    emptyImmutableNCC, emptyImmutableNCHC,
    emptyImmutableNCC, emptyImmutableNCHC,
    emptyImmutableNCC, 0L)


  private def printNCC(map: NamedCountersCollection, span: Long): String = map.toSeq.foldLeft("") {
    case (agg, (name, value)) => agg.concat(f"\n\t\t\t\t$name -> $value\t ${value.toDouble / span}%.2f/sec")
  }

  private def printNCHC(map: NamedCountersHyperCollection, span: Long): String = map.toSeq.foldLeft("") {
    case (agg, (name, counterCollection)) => agg.concat(s"\n\t\t\t$name :: ${printNCC(counterCollection, span)}")
  }

  // implicit classes lets us define our own methods on existing classes - like unions of Map[String, Long]
  implicit class StringLongMapExtension(val map: Map[String, Long]) {
    def unionWith(that: StringLongMapExtension): Map[String, Long] = {
      val kvPairsConcat: Seq[(String, Long)] = this.map.toList ++ that.map.toList
      val groupedByKey = kvPairsConcat.groupBy(_._1)
      val mergedByValue = groupedByKey.map { case (key, kvSeq) => key -> kvSeq.foldLeft(0L)(_ + _._2) }
      mergedByValue
    }
  }

  implicit class StringMapStringLongExtension(val map: Map[String, Map[String, Long]]) {
    def unionWith(that: StringMapStringLongExtension): Map[String, Map[String, Long]] = {
      val kvPairsConcat: Seq[(String, Map[String, Long])] = this.map.toList ++ that.map.toList
      val groupedByKey = kvPairsConcat.groupBy(_._1)
      val mergedByValue = groupedByKey.map { case (key, strAndNamedCountersSeq) =>
        key -> strAndNamedCountersSeq.foldLeft(emptyImmutableNCC)(_ unionWith _._2)
      }
      mergedByValue
    }
  }

  implicit def logEventToAggregatedMetrics(event: LogEvent): AggregatedMetrics = {
    val eventsWindow = event.as[EventsWindow]

    val oneStatus = Map(event.status -> 1L)
    val oneHttpMethod = Map(event.request.method -> 1L)
    val oneHttpMethodWithStatus = Map(event.request.method -> oneStatus)
    val oneHost = Map(event.host -> 1L)
    val oneHostWithStatus = Map(event.host -> oneStatus)
    val oneSection = event.request.section.fold(Map.empty[String, Long])(section => Map(section -> 1L))
    val oneUser = Map(event.authUser -> 1L)
    val oneSectionWithStatus = event.request.section.fold(Map.empty[String, Map[String, Long]])(section => Map(section -> oneStatus))
    val oneUserWithStatus = Map(event.authUser -> oneStatus)

    AggregatedMetrics(eventsWindow,
      oneHttpMethod, oneHttpMethodWithStatus,
      oneHost, oneHostWithStatus,
      oneSection, oneSectionWithStatus,
      oneUser, oneUserWithStatus,
      oneStatus, event.bytes)
  }

}
