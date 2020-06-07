package com.joeybaruch.datamodel

import scala.math.{max, min}

object AggregatedMetrics {

  type NamedCountersCollection = Map[String, Long]
  type NamedCountersHyperCollection = Map[String, Map[String, Long]]

  // implicit class lets us define our own methods on existing classes - like Map[String, Long]
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
        key -> strAndNamedCountersSeq.foldLeft(Map.empty[String, Long])(_ unionWith _._2)
      }
      mergedByValue
    }
  }

  case class BaseAggregatedMetrics(/** ****** window info *********/
                                   eventCount: Long,
                                   earliestTimestamp: Long,
                                   latestTimestamp: Long,

                                   /** ****** business metrics *********/
                                   sectionCounters: NamedCountersCollection) {

    val timeSpan: Long = latestTimestamp - earliestTimestamp


    def +(that: BaseAggregatedMetrics): BaseAggregatedMetrics = {
      val newEventCount = this.eventCount + that.eventCount
      val newEarliestTimestamp = min(this.earliestTimestamp, that.earliestTimestamp)
      val newLatestTimestamp = max(this.latestTimestamp, that.latestTimestamp)
      val newSectionCounters = this.sectionCounters unionWith that.sectionCounters

      BaseAggregatedMetrics(newEventCount, newEarliestTimestamp, newLatestTimestamp, newSectionCounters)
    }

  }


  case class DebugAggregatedMetrics(baseAggregatedMetrics: BaseAggregatedMetrics,

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
    def +(that: DebugAggregatedMetrics): DebugAggregatedMetrics = {
      val newBaseAggMetrics = this.baseAggregatedMetrics + that.baseAggregatedMetrics

      val newHttpMethodsCounters = this.httpMethodsCounters unionWith that.httpMethodsCounters
      val newHttpMethodResponseStatusCounters = this.httpMethodResponseStatusCounters unionWith that.httpMethodResponseStatusCounters

      val newHostsCounters = this.hostsCounters unionWith that.hostsCounters
      val newHostResponseStatusCounters = this.hostResponseStatusCounters unionWith that.hostResponseStatusCounters

      val newSectionResponseStatusCounters = this.sectionResponseStatusCounters unionWith that.sectionResponseStatusCounters

      val newUsersCounters = this.usersCounters unionWith that.usersCounters
      val newUserResponseStatusCounters = this.userResponseStatusCounter unionWith that.userResponseStatusCounter

      val newStatusCounters = this.statusCounters unionWith that.statusCounters
      val newBytesCounter = this.bytesCounter + that.bytesCounter

      DebugAggregatedMetrics(newBaseAggMetrics,
        newHttpMethodsCounters, newHttpMethodResponseStatusCounters,
        newHostsCounters, newHostResponseStatusCounters,
        newSectionResponseStatusCounters,
        newUsersCounters, newUserResponseStatusCounters,
        newStatusCounters, newBytesCounter)
    }

    def truncate: BaseAggregatedMetrics = this.baseAggregatedMetrics
  }

}
