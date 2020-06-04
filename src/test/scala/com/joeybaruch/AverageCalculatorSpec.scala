package com.joeybaruch

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import com.joeybaruch.datamodel.{InvalidReading, ValidReading}
import com.joeybaruch.importer.CsvImporter
import com.joeybaruch.repository.ReadingRepository
import com.typesafe.config.ConfigFactory
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class AverageCalculatorSpec extends AnyFlatSpec with Matchers {

  implicit val system = ActorSystem("test")
  implicit val materializer = ActorMaterializer()

  val tested = new CsvImporter(ConfigFactory.load(), new ReadingRepository).computeAverage

  it should "count an average of valid readings" in {
    // given
    val readings = List(
      ValidReading(1, 2),
      ValidReading(1, 3),
      ValidReading(2, 4),
      ValidReading(2, 5)
    )

    // when
    val flow = Source(readings).via(tested)

    // then
    flow.runWith(TestSink.probe[ValidReading])
      .request(4)
      .expectNextUnordered(ValidReading(1, 2.5), ValidReading(2, 4.5))
      .expectComplete()
  }

  it should "count an average of mixed valid and invalid readings" in {
    // given
    val readings = List(
      ValidReading(1, 2),
      InvalidReading(1),
      InvalidReading(2),
      ValidReading(2, 5)
    )

    // when
    val flow = Source(readings).via(tested)

    // then
    flow.runWith(TestSink.probe[ValidReading])
      .request(4)
      .expectNextUnordered(ValidReading(1, 2), ValidReading(2, 5))
      .expectComplete()
  }

  it should "count a fake average of invalid readings" in {
    // given
    val readings = List(
      InvalidReading(1),
      InvalidReading(1)
    )

    // when
    val flow = Source(readings).via(tested)

    // then
    flow.runWith(TestSink.probe[ValidReading])
      .request(2)
      .expectNext(ValidReading(1, -1))
      .expectComplete()
  }
}
