package com.joeybaruch.datamodel

import scala.math.{max, min}

trait AggregatedMetricsMonoid[A] {
  def empty: A

  def combine(aggMetrics1: A, aggMetrics2: A): A

  def aggregate(seq: Seq[A]):A = seq.foldLeft(empty)(combine)
}

object AggregatedMetrics {

  def aggregate[T: AggregatedMetricsMonoid](seq: T*): T = implicitly[AggregatedMetricsMonoid[T]].aggregate(seq)


  /** *************     Implicit conversions from our metrics containers to our monoid        **************/
  implicit def baseMetricsMonoid[A]: AggregatedMetricsMonoid[BaseAggMetrics] =
    new AggregatedMetricsMonoid[BaseAggMetrics] {
      override def empty: BaseAggMetrics = emptyBaseAggMetrics

      override def combine(aggMetrics1: BaseAggMetrics, aggMetrics2: BaseAggMetrics): BaseAggMetrics =
        aggMetrics1 + aggMetrics2
    }

  implicit def debugMetricsMonoid[A]: AggregatedMetricsMonoid[DebugAggMetrics] =
    new AggregatedMetricsMonoid[DebugAggMetrics] {
      override def empty: DebugAggMetrics = emptyDebugAggMetrics

      override def combine(aggMetrics1: DebugAggMetrics, aggMetrics2: DebugAggMetrics): DebugAggMetrics =
        aggMetrics1 + aggMetrics2
    }

  type NamedCountersCollection = Map[String, Long]
  type NamedCountersHyperCollection = Map[String, NamedCountersCollection]

  /** *************     Empty Units for Monoid        **************/
  private def emptyImmutableNCC: NamedCountersCollection = Map.empty[String, Long]

  private def emptyImmutableNCHC: NamedCountersHyperCollection = Map.empty[String, NamedCountersCollection]

  private val emptyBaseAggMetrics = BaseAggMetrics(0L, Long.MaxValue, Long.MinValue, emptyImmutableNCC)

  private val emptyDebugAggMetrics = DebugAggMetrics(emptyBaseAggMetrics, emptyImmutableNCC, emptyImmutableNCHC,
    emptyImmutableNCC, emptyImmutableNCHC,
    emptyImmutableNCHC,
    emptyImmutableNCC, emptyImmutableNCHC,
    emptyImmutableNCC, 0L)


  /** *************     Light and heavy (base & debug) containers for metrics        **************/
  case class BaseAggMetrics(/** ****** window info *********/
                            eventCount: Long,
                            earliestTimestamp: Long,
                            latestTimestamp: Long,

                            /** ****** business metrics *********/
                            sectionCounters: NamedCountersCollection) {

    val timeSpan: Long = latestTimestamp - earliestTimestamp


    def +(that: BaseAggMetrics): BaseAggMetrics = {
      val newEventCount = this.eventCount + that.eventCount
      val newEarliestTimestamp = min(this.earliestTimestamp, that.earliestTimestamp)
      val newLatestTimestamp = max(this.latestTimestamp, that.latestTimestamp)
      val newSectionCounters = this.sectionCounters unionWith that.sectionCounters

      BaseAggMetrics(newEventCount, newEarliestTimestamp, newLatestTimestamp, newSectionCounters)
    }

  }


  case class DebugAggMetrics(baseAggregatedMetrics: BaseAggMetrics,

                             /** ****** business metrics *********/
                             httpMethodsCounters: NamedCountersCollection,
                             // could specific http methods/verbs be having negative trends?
                             httpMethodResponseStatusCounters: NamedCountersHyperCollection,

                             hostsCounters: NamedCountersCollection,
                             // could specific hosts have bad response trends?
                             hostResponseStatusCounters: NamedCountersHyperCollection,

                             // could specific sections have bad response trends?
                             sectionResponseStatusCounters: NamedCountersHyperCollection,

                             usersCounters: NamedCountersCollection,
                             // could specific users have bad response trends?
                             userResponseStatusCounter: NamedCountersHyperCollection,

                             statusCounters: NamedCountersCollection,
                             bytesCounter: Long
                            ) {
    def +(that: DebugAggMetrics): DebugAggMetrics = {
      val newBaseAggMetrics = (this.baseAggregatedMetrics + that.baseAggregatedMetrics)

      val newHttpMethodsCounters = this.httpMethodsCounters unionWith that.httpMethodsCounters
      val newHttpMethodResponseStatusCounters = this.httpMethodResponseStatusCounters unionWith that.httpMethodResponseStatusCounters

      val newHostsCounters = this.hostsCounters unionWith that.hostsCounters
      val newHostResponseStatusCounters = this.hostResponseStatusCounters unionWith that.hostResponseStatusCounters

      val newSectionResponseStatusCounters = this.sectionResponseStatusCounters unionWith that.sectionResponseStatusCounters

      val newUsersCounters = this.usersCounters unionWith that.usersCounters
      val newUserResponseStatusCounters = this.userResponseStatusCounter unionWith that.userResponseStatusCounter

      val newStatusCounters = this.statusCounters unionWith that.statusCounters
      val newBytesCounter = this.bytesCounter + that.bytesCounter

      DebugAggMetrics(newBaseAggMetrics,
        newHttpMethodsCounters, newHttpMethodResponseStatusCounters,
        newHostsCounters, newHostResponseStatusCounters,
        newSectionResponseStatusCounters,
        newUsersCounters, newUserResponseStatusCounters,
        newStatusCounters, newBytesCounter)
    }

    def truncate: BaseAggMetrics = this.baseAggregatedMetrics
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

}
