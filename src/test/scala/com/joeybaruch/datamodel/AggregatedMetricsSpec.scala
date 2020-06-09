package com.joeybaruch.datamodel

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.collection.mutable

class AggregatedMetricsSpec extends AnyFlatSpec with Matchers {

  import AggregatedMetrics._

  behavior of "AggregatedMetricsMonoid"

  it should "aggregate via the monoid abstraction" in {
    AggregatedMetrics.aggregate(Seq(bag1, bag2)) should equal(bag1Plus2)
    AggregatedMetrics.aggregate(Seq(full1, full2)) should equal(full1Plus2)
  }

  behavior of "AggregatedMetricsObject"

  it should "aggregate base metrics correctly" in {
    bag1 + bag2 should equal(bag1Plus2)
  }

  it should "aggregate full metrics correctly" in {
    full1 + full2 should equal(full1Plus2)
  }

  it should "truncate the superset into the subset" in {
    full1.truncate should equal(bag1)
  }


  behavior of "map extensions"

  they should "concatenate mutually exclusive maps" in {
    oneSection1 unionWith oneSection2 should equal(oneSection1 ++ oneSection2)
    getWithOneOk unionWith putWithOneNotFound should equal(getWithOneOk ++ putWithOneNotFound)
  }

  they should "unite partially inclusive maps" in {
    (oneSection1 ++ oneSection2) unionWith oneSection1 should equal(twoSection1 ++ oneSection2)
    (getWithOneOk ++ putWithOneNotFound) unionWith putWithOneNotFound should equal(getWithOneOk ++ putWithTwoNotFound)
    (getWithOneOk ++ putWithOneOk) unionWith putWithOneOk should equal(getWithOneOk ++ putWithTwoOk)
  }

  they should "merge mutually inclusive sets" in {
    oneSection1 unionWith oneSection1 should equal(twoSection1)
    putWithOneOk unionWith putWithOneOk should equal(putWithTwoOk)
  }

  private lazy val oneSection1 = Map("s1" -> 1L)
  private lazy val twoSection1 = Map("s1" -> 2L)
  private lazy val oneSection2 = Map("s2" -> 1L)

  lazy val bag1: BaseAggMetrics = BaseAggMetrics(1, 1L, 1L)
  lazy val bag2: BaseAggMetrics = BaseAggMetrics(1, 2L, 2L)
  lazy val bag1Plus2: BaseAggMetrics = BaseAggMetrics(2, 1L, 2L)

  private lazy val oneGet = Map("get" -> 1L)
  private lazy val onePut = Map("put" -> 1L)
  private lazy val oneOkStatus = Map("200" -> 1L)
  private lazy val twoOkStatus = Map("200" -> 2L)
  private lazy val oneNotFoundStatus = Map("404" -> 1L)
  private lazy val twoNotFoundStatus = Map("404" -> 2L)
  private lazy val oneOkOneNotFound = oneOkStatus ++ oneNotFoundStatus
  private lazy val getWithOneOk = Map("get" -> oneOkStatus)
  private lazy val putWithOneOk = Map("put" -> oneOkStatus)
  private lazy val putWithTwoOk = Map("put" -> twoOkStatus)
  private lazy val putWithOneNotFound = Map("put" -> oneNotFoundStatus)
  private lazy val putWithTwoNotFound = Map("put" -> twoNotFoundStatus)
  private lazy val oneHost1 = Map("h1" -> 1L)
  private lazy val twoHost1 = Map("h1" -> 2L)
  private lazy val host1WithOneOk = Map("h1" -> oneOkStatus)
  private lazy val host1WithOneNotFound = Map("h1" -> oneNotFoundStatus)
  private lazy val section1WithOneOk = Map("s1" -> oneOkStatus)
  private lazy val section1WithOneNotFound = Map("s1" -> oneNotFoundStatus)
  private lazy val oneUser1 = Map("u1" -> 1L)
  private lazy val twoUser1 = Map("u1" -> 2L)
  private lazy val oneUser1WithOneOk = Map("u1" -> oneOkStatus)
  private lazy val oneUser1WithOneNotFound = Map("u1" -> oneNotFoundStatus)

  lazy val full1: AggMetrics = AggMetrics(bag1,
    oneGet, getWithOneOk,
    oneHost1, host1WithOneOk,
    oneSection1, section1WithOneOk,
    oneUser1, oneUser1WithOneOk,
    oneOkStatus, 10L)
  lazy val full2: AggMetrics = AggMetrics(bag2,
    onePut, putWithOneNotFound,
    oneHost1, host1WithOneNotFound,
    oneSection1, section1WithOneNotFound,
    oneUser1, oneUser1WithOneNotFound,
    oneNotFoundStatus, 10L)
  lazy val full1Plus2: AggMetrics = AggMetrics(bag1Plus2,
    oneGet ++ onePut, Map("get" -> oneOkStatus, "put" -> oneNotFoundStatus),
    twoHost1, Map("h1" -> oneOkOneNotFound),
    twoSection1, Map("s1" -> oneOkOneNotFound),
    twoUser1, Map("u1" -> oneOkOneNotFound),
    oneOkOneNotFound, 20L)
}
