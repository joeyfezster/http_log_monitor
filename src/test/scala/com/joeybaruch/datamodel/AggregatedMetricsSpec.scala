package com.joeybaruch.datamodel

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class AggregatedMetricsSpec extends AnyFlatSpec with Matchers {

  import AggregatedMetrics._

  behavior of "AggregatedMetricsMonoid"

  it should "aggregate via the monoid abstraction" in {
    aggregate(b1, b2) should equal(b1PlusB2)
    aggregate(db1, db2) should equal(db1Plus2)
  }


  behavior of "AggregatedMetricsObject"

  it should "aggregate base metrics correctly" in {
    b1 + b2 should equal(b1PlusB2)
  }

  it should "aggregate debug metrics correctly" in {
    db1 + db2 should equal(db1Plus2)
  }

  it should "truncate the superset into the subset" in {
    db1.truncate should equal(b1)
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

  lazy val b1: BaseAggMetrics = BaseAggMetrics(1, 1L, 1L, oneSection1)
  lazy val b2: BaseAggMetrics = BaseAggMetrics(1, 2L, 2L, oneSection1 ++ oneSection2)
  lazy val b1PlusB2: BaseAggMetrics = BaseAggMetrics(2, 1L, 2L, twoSection1 ++ oneSection2)

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

  lazy val db1: DebugAggMetrics = DebugAggMetrics(b1,
    oneGet, getWithOneOk,
    oneHost1, host1WithOneOk,
    section1WithOneOk,
    oneUser1, oneUser1WithOneOk,
    oneOkStatus, 10L)
  lazy val db2: DebugAggMetrics = DebugAggMetrics(b2,
    onePut, putWithOneNotFound,
    oneHost1, host1WithOneNotFound,
    section1WithOneNotFound,
    oneUser1, oneUser1WithOneNotFound,
    oneNotFoundStatus, 10L)
  lazy val db1Plus2: DebugAggMetrics = DebugAggMetrics(b1PlusB2,
    oneGet ++ onePut, Map("get" -> oneOkStatus, "put" -> oneNotFoundStatus),
    twoHost1, Map("h1" -> oneOkOneNotFound),
    Map("s1" -> oneOkOneNotFound),
    twoUser1, Map("u1" -> oneOkOneNotFound),
    oneOkOneNotFound, 20L)
}
