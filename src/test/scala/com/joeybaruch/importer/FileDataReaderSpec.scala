package com.joeybaruch.importer

import akka.Done
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import org.scalatest.GivenWhenThen

import scala.concurrent.duration._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.Await

class FileDataReaderSpec extends AnyFlatSpec with Matchers with GivenWhenThen with LazyLogging{

  implicit val system = ActorSystem()

  val config = ConfigFactory.load()

  behavior of "cvsParser"
  it should "parse a bytestream" in {
    Given("a config pointing to a directory with logfiles")
    And("An Akka Graph to read and parse them")
    val sinkUnderTest = new FileDataReader(config, new LogLineParser(config)).importFromFiles

    When("it's run")
    val result = Await.result(sinkUnderTest, 3.seconds)
    assert(result == Done)
  }


  behavior of "FileDataReader import flow"

  it should "parse a csv file to the model data case classes" in {

  }
}
