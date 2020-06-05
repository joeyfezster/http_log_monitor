package com.joeybaruch.monitor

import java.io.{File, FileInputStream}
import java.nio.file.Paths

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.alpakka.csv.scaladsl.CsvParsing
import akka.stream.scaladsl.{Flow, Framing, Source, StreamConverters}
import akka.util.ByteString
import com.joeybaruch.datamodel.{LogEvent, LogLine}
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import org.apache.commons.lang3.StringEscapeUtils

import scala.util.Properties

class FileDataReader(config: Config, parser: LogParser)
                    (implicit system: ActorSystem) extends LazyLogging {


  def processFile(filepath: String): Source[LogEvent, NotUsed] = {
    logger.info(s"processing file: $filepath")
    val file = Paths.get(filepath).toFile
    Source(Seq(file))
      .via(processSingleFile)
  }

  lazy val processSingleFile: Flow[File, LogEvent, NotUsed] =
    Flow[File]
      .via(parseFile)
      .via(collectLogEvents)

  val parseFile: Flow[File, LogLine, NotUsed] =
    Flow[File].flatMapConcat { file =>
      val fileInputStream = new FileInputStream(file)

      StreamConverters.fromInputStream(() => fileInputStream)
        .via(csvParser)
        .map(parseLine(file.getPath))
    }

  val collectLogEvents: Flow[LogLine, LogEvent, NotUsed] =
    Flow[LogLine]. /*windowing?*/ collect { case logEvent: LogEvent => logEvent } //todo: add windowing here?

  val csvParser: Flow[ByteString, List[String], NotUsed] =
    CsvParsing.lineScanner(delimiter = ',', quoteChar = '"', escapeChar = ' ')
      .map(byteSrtingList => byteSrtingList.map(_.utf8String))

  private def parseLine(filePath: String)(columns: List[String]): LogLine = parser.parse(columns, Some(filePath))
}
