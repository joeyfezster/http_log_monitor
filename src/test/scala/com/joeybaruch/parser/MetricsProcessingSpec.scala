package com.joeybaruch.parser

import java.nio.file.Paths

import akka.actor.ActorSystem
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class MetricsProcessingSpec extends AnyFlatSpec with Matchers {

  implicit val system: ActorSystem = ActorSystem()

  //  behavior of "mc"
  //
  //
  //  it should "report to the console" in {
  //    val consoleReporter = ConsoleReporter.forRegistry(metricRegistry)
  //      .convertRatesTo(TimeUnit.SECONDS)
  //      .convertDurationsTo(TimeUnit.MILLISECONDS)
  //      .build()
  //
  //    val config = ConfigFactory.load()
  //    val dataReader = new FileDataReader(config, new ColumnarLogParser(config)).processFile(fullSample)
  //    val timeAligner = new EventFlowAligner(config).timeAligned
  //    val metricsCapture = new MetricsProcessing().metricsCapture
  //
  //    val graph = dataReader.via(timeAligner).via(metricsCapture).runWith(Sink.seq)
  //    val result = Await.result(graph, 10.seconds)
  //
  //
  //
  //    Thread.sleep(5*1000)
  //    consoleReporter.report()
  //
  //  }
  //
  //  it should "tesitnt" in {
  //    val consoleReporter = ConsoleReporter.forRegistry(metricRegistry)
  //      .convertRatesTo(TimeUnit.SECONDS)
  //      .convertDurationsTo(TimeUnit.MILLISECONDS)
  //      .build()
  //    consoleReporter.start(0, 1, TimeUnit.SECONDS)
  //
  //    metrics.counter("test").inc()
  //
  //    Thread.sleep(5*1000)
  //
  //  }
  lazy val fullSample: String = Paths.get(getClass.getResource("/sample/sample_csv.txt").toURI).toString

}
