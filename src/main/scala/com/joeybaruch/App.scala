package com.joeybaruch

import java.nio.file.Paths

import akka.Done
import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.io.StdIn
import scala.util.{Failure, Success, Try}


object App extends LazyLogging {
  //todo: customize execution context by execution hardware (profiling + knowledge of hw)
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  val usage: String =
    """Usage: java [-D<option=value> ...] -jar <path-to-jar> <path-to-logfile>
      |shell piping usage: <cmd that outputs file location> | java [-D<option=value>...] -jar <path-to-jar>
      |Configs: application.conf file is packaged with the jar, and can be overridden with environment variables;
      |for example:
      |unix: java -Dmonitor.show-debug-stats=TRUE -Dmetrics.report-every.seconds=100 -jar <path-to-jar> <path-to-logfile>
      |windows: java "-Dmonitor.show-debug-stats=TRUE" "-Dmetrics.report-every.seconds=100" -jar <path-to-jar> <path-to-logfile>
      |""".stripMargin

  def main(args: Array[String]): Unit = {
    val inputFileOrDir = parseArgs(args)

    implicit val system: ActorSystem = ActorSystem()
    val config = ConfigFactory.load()

    // parallel processing of multiple files - feature toggle
    val multipleFilesEnabled = false
    if (!multipleFilesEnabled) {
      singleFileProcess(inputFileOrDir, config)
    } else {
      parallelProcessingOfDirectory(inputFileOrDir, config)
    }
  }


  private def singleFileProcess(inputFileOrDir: String, config: Config)(implicit system: ActorSystem): Unit = {
    val fileToProcess = getFileOrExit(inputFileOrDir)
    logger.info(s"processing $fileToProcess")

    val (alertsFuture, metricsFuture) = HttpLogMonitor.runForFile(fileToProcess, config)
    handleProcessingResult(alertsFuture, metricsFuture)
  }

  private def parallelProcessingOfDirectory(inputFileOrDir: String, config: Config)(implicit system: ActorSystem): Unit = {
    /**
     * Clarification : this "dead code" only serves to show how easily this project could process multiple log files
     * in parallel. Some simple modifications are still required, for example, adding a FileReporter s.t. the output
     * for each file goes to a file and not all to the console.
     **/
    val directory = getFileOrExit(inputFileOrDir)

    val allFilesFutures = directory.listFiles().toList
      .map(file => HttpLogMonitor.runForFile(file, config))
      .map { case (alertsFuture, metricsFuture) => alertsFuture zip metricsFuture }

    Future.sequence(allFilesFutures).onComplete(_ => system.terminate())
  }

  private def parseArgs(args: Array[String]): String = {
    if (args.length == 0) {
      //taking input from pipeline, not interactive mode
      val line = Try(Await.result(Future[String](StdIn.readLine()), 100.milliseconds)).getOrElse("")
      line match {
        case str: String if str.nonEmpty => str.trim
        case _ => println(usage); sys.exit(1)
      }
    }
    else {
      args(0) match {
        case "-h" | "-help" | "--help" =>
          println(usage)
          sys.exit(0)
        case arg: String => arg
      }
    }
  }

  private def getFileOrExit(filepath: String) = {
    try {
      Paths.get(filepath).toFile
    } catch {
      case e: Throwable =>
        val msg = s"Failed to extract file from path: $filepath"
        logPrintExit(msg, e, 4)
    }
  }

  private def handleProcessingResult(alertsFuture: Future[Done], metricsFuture: Future[Done])(implicit
                                                                                              system: ActorSystem): Unit = {
    (alertsFuture zip metricsFuture).onComplete {
      case Success(_) =>
        logger.info("Successfully processed alerts and metrics")
        system.terminate()
      case Failure(ex) =>
        val msg = s"Error processing file: $ex"
        logPrintExit(msg, ex, 2)
    }
  }

  private def logPrintExit(msg: String, throwable: Throwable, code: Int) = {
    logger.error(msg, throwable)
    println(msg)
    sys.exit(code)
  }
}
