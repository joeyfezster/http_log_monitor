package com.joeybaruch.datamodel

import scala.math.{max, min}


object AggregatedMetrics {

  def aggregate[T: AggregatedMetricsMonoid](seq: Seq[T]): T = implicitly[AggregatedMetricsMonoid[T]].aggregate(seq)


  /** *************     Implicit monoids for our Aggregated Metrics containers        **************/
  implicit val baseMetricsMonoid: AggregatedMetricsMonoid[BaseAggMetrics] =
    new AggregatedMetricsMonoid[BaseAggMetrics] {
      override def empty: BaseAggMetrics = emptyBaseAggMetrics

      override def combine(aggMetrics1: BaseAggMetrics, aggMetrics2: BaseAggMetrics): BaseAggMetrics =
        aggMetrics1 + aggMetrics2
    }

  implicit val aggMetricsMonoid: AggregatedMetricsMonoid[AggMetrics] =
    new AggregatedMetricsMonoid[AggMetrics] {
      override def empty: AggMetrics = emptyAggMetrics

      override def combine(aggMetrics1: AggMetrics, aggMetrics2: AggMetrics): AggMetrics =
        aggMetrics1 + aggMetrics2
    }

  type NamedCountersCollection = Map[String, Long]
  type NamedCountersHyperCollection = Map[String, NamedCountersCollection]

  /** *************     Empty Units for Monoids        **************/
  private def emptyImmutableNCC: NamedCountersCollection = Map.empty[String, Long]

  private def emptyImmutableNCHC: NamedCountersHyperCollection = Map.empty[String, NamedCountersCollection]

  val emptyBaseAggMetrics: BaseAggMetrics = BaseAggMetrics(0L, Long.MaxValue, Long.MinValue)

  val emptyAggMetrics: AggMetrics = AggMetrics(emptyBaseAggMetrics,
    emptyImmutableNCC, emptyImmutableNCHC,
    emptyImmutableNCC, emptyImmutableNCHC,
    emptyImmutableNCC, emptyImmutableNCHC,
    emptyImmutableNCC, emptyImmutableNCHC,
    emptyImmutableNCC, 0L)


  /** *************     Light and heavy (base & full) containers for metrics        **************/
  case class BaseAggMetrics(/** ****** window info *********/
                            eventCount: Long,
                            earliestTimestamp: Long,
                            latestTimestamp: Long) {

    val timeSpan: Long = latestTimestamp - earliestTimestamp


    def +(that: BaseAggMetrics): BaseAggMetrics = {
      val newEventCount = this.eventCount + that.eventCount
      val newEarliestTimestamp = min(this.earliestTimestamp, that.earliestTimestamp)
      val newLatestTimestamp = max(this.latestTimestamp, that.latestTimestamp)

      BaseAggMetrics(newEventCount, newEarliestTimestamp, newLatestTimestamp)
    }

  }


  case class AggMetrics(baseAggregatedMetrics: BaseAggMetrics,

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
    def +(that: AggMetrics): AggMetrics = {
      val newBaseAggMetrics = (this.baseAggregatedMetrics + that.baseAggregatedMetrics)

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

      AggMetrics(newBaseAggMetrics,
        newHttpMethodsCounters, newHttpMethodResponseStatusCounters,
        newHostsCounters, newHostResponseStatusCounters,
        newSectionCounters, newSectionResponseStatusCounters,
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
