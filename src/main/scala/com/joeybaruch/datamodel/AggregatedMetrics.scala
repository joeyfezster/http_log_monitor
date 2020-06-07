package com.joeybaruch.datamodel

import scala.math.{max, min}

object AggregatedMetrics {

  //  trait AggregatableMetric{
  //    val eventCount: Long
  //    val earliestTimestamp: Long
  //    val latestTimestamp: Long
  //
  //    lazy val timeSpan = latestTimestamp - earliestTimestamp
  //
  //    def +(that: AggregatableMetric): AggregatableMetric
  //  }

  // implicit class lets us define our own methods on existing classes - like Map[String, Long]
  implicit class NamedCountersCollection(val map: Map[String, Long]) {
    def unionWith(that: NamedCountersCollection): Map[String, Long] = {
      val kvPairsConcat: Seq[(String, Long)] = this.map.toList ++ that.map.toList
      val groupedByKey = kvPairsConcat.groupBy(_._1)
      val mergedByValue = groupedByKey.map { case (key, kvSeq) => key -> kvSeq.foldLeft(0L)(_ + _._2) }
      mergedByValue
    }
  }

  implicit class NamedCountersHypercollection(val map: Map[String, Map[String, Long]]) {
    def unionWith(that: NamedCountersHypercollection): Map[String, Map[String, Long]] = {
      val kvPairsConcat: Seq[(String, Map[String, Long])] = this.map.toList ++ that.map.toList
      val groupedByKey = kvPairsConcat.groupBy(_._1)
      val mergedByValue = groupedByKey.map { case (key, strAndNamedCountersSeq) =>
        key -> strAndNamedCountersSeq.foldLeft(Map.empty[String, Long])(_ unionWith _._2)
      }
      mergedByValue
    }
  }

  case class TruncatedAggregatedMetrics(val eventCount: Long,
                                   val earliestTimestamp: Long,
                                   val latestTimestamp: Long,
                                   val sectionCounters: NamedCountersCollection) {

    val timeSpan: Long = latestTimestamp - earliestTimestamp

    def +(that: TruncatedAggregatedMetrics): TruncatedAggregatedMetrics = {
      //todo - eli, how do I not repeat myself here?
      val newEventCount = this.eventCount + that.earliestTimestamp
      val newEarliestTimestamp = min(this.earliestTimestamp, that.earliestTimestamp)
      val newLatestTimestamp = max(this.latestTimestamp, that.latestTimestamp)
      val newSectionCounters = this.sectionCounters unionWith that.sectionCounters

      new TruncatedAggregatedMetrics(newEventCount, newEarliestTimestamp, newLatestTimestamp, newSectionCounters)
    }

  }

  //
  //  case class ThinAggregatedMetrics(
  //                                    override val representedEventCount: Long,
  //                                    override val earliestTimestamp: Long,
  //                                    override val latestTimestamp: Long,
  //
  //                                    sectionCounter: NamedCounterCollection
  //                                  ) extends AggregatedMetrics(representedEventCount, earliestTimestamp, latestTimestamp) {
  //

  //
  //    def +(that: ThinAggregatedMetrics): ThinAggregatedMetrics = {
  //      val upper = super.+(that)
  //      val mergedSectionCounter = mergeMultipleChoiceCounters(this.sectionCounter, that.sectionCounter)
  //      ThinAggregatedMetrics(upper.representedEventCount, upper.earliestTimestamp, upper.latestTimestamp, mergedSectionCounter)
  //    }
  //  }

  case class DebugAggregatedMetrics(
                                /** ****** window info *********/
                                val eventCount: Long,
                                val earliestTimestamp: Long,
                                val latestTimestamp: Long,

                                /** ****** business metrics *********/
                                val httpMethodsCounters: NamedCountersCollection,
                                // could specifc http methods/verbs be having negative trends?
                                val httpMethodResponseStatusCounters: NamedCountersHypercollection,

                                val hostsCounters: NamedCountersCollection,
                                // could specific hosts have bad response trends?
                                val hostResponseStatusCounters: NamedCountersHypercollection,

                                val sectionsCounters: NamedCountersCollection,
                                // could specific sections have bad response trends?
                                val sectionResponseStatusCounters: NamedCountersHypercollection,

                                val usersCounters: NamedCountersCollection,
                                // could specific users have bad response trends?
                                val userResponseStatusCounter: NamedCountersHypercollection,

                                val statusCounters: NamedCountersCollection,
                                val bytesCounter: Long
                              ) {
    val timeSpan: Long = latestTimestamp - earliestTimestamp


    def +(that: DebugAggregatedMetrics): DebugAggregatedMetrics = {
      val newEventCount = this.eventCount + that.eventCount
      val newEarliestTimestamp = min(this.earliestTimestamp, that.earliestTimestamp)
      val newLatestTimestamp = max(this.latestTimestamp, that.latestTimestamp)

      val newHttpMethodsCounters = this.httpMethodsCounters unionWith that.httpMethodsCounters
      val newHttpMethodResponseStatusCounters = this.httpMethodResponseStatusCounters unionWith that.httpMethodResponseStatusCounters

      val newHostsCounters = this.hostsCounters unionWith that.hostsCounters
      val newHostResponseStatusCounters = this.hostResponseStatusCounters unionWith that.hostResponseStatusCounters

      val newSectionsCounters = this.sectionsCounters unionWith that.sectionsCounters
      val newSectionResponseStatusCounters = this.sectionResponseStatusCounters unionWith that.sectionResponseStatusCounters

      val newUsersCounters = this.usersCounters unionWith that.usersCounters
      val newUserResponseStatusCounters = this.userResponseStatusCounter unionWith that.userResponseStatusCounter

      val newStatusCounters = this.statusCounters unionWith that.statusCounters
      val newBytesCounter = this.bytesCounter + that.bytesCounter

      new DebugAggregatedMetrics(newEventCount, newEarliestTimestamp, newLatestTimestamp, newHttpMethodsCounters,
        newHttpMethodResponseStatusCounters, newHostsCounters, newHostResponseStatusCounters, newSectionsCounters,
        newSectionResponseStatusCounters, newUsersCounters, newUserResponseStatusCounters, newStatusCounters, newBytesCounter)
    }

    def truncate: TruncatedAggregatedMetrics =
      new TruncatedAggregatedMetrics(this.eventCount, this.earliestTimestamp, this.latestTimestamp, this.sectionsCounters)
  }

}
