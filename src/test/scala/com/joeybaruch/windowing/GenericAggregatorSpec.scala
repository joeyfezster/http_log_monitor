package com.joeybaruch.windowing

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class GenericAggregatorSpec extends AnyFlatSpec with Matchers with BeforeAndAfter{

  implicit val system: ActorSystem = ActorSystem()
  var config: Config = _
  var eventFlowAligner = EventFlowAligner
  var allowedDelay: Long = _
  var minUnalowedDelay: Long = _

  before {
    config = ConfigFactory.load("test-conf")
//    allowedDelay = config.getInt("windowing.late-data.delay-allowed.seconds").seconds.toSeconds
    minUnalowedDelay = allowedDelay + 1
  }

  behavior of "one second aggregator"
  it should "aggregate an ordered stream of log events to aggregated metrics worth one second each" in{

  }

}
