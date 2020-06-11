package com.joeybaruch.parser

import java.io.{File, FileInputStream}

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.alpakka.csv.scaladsl.CsvParsing
import akka.stream.scaladsl.{Flow, Source, StreamConverters}
import akka.util.ByteString
import com.joeybaruch.datamodel.LegalLogEvent.{LogEvent, SentinelEOFEvent}
import com.joeybaruch.datamodel.{LegalLogEvent, LogLine}
import com.typesafe.scalalogging.LazyLogging

class FileDataReader(parser: LogParser)(implicit system: ActorSystem) extends LazyLogging {

  def fileSource(file: File): Source[LogEvent, NotUsed] = {
    Source(Seq(file))
      .via(processSingleFile)
  }.concat(Source.single(SentinelEOFEvent))


  private lazy val processSingleFile: Flow[File, LogEvent, NotUsed] =
    Flow[File]
      .via(parseFile)
      .via(collectLogEvents)

  private lazy val parseFile: Flow[File, LogLine, NotUsed] =
    Flow[File].flatMapConcat { file =>
      val fileInputStream = new FileInputStream(file)

      StreamConverters.fromInputStream(() => fileInputStream)
        .via(csvScanner)
        .map(parseLine(file.getPath))
    }

  private lazy val collectLogEvents: Flow[LogLine, LogEvent, NotUsed] =
    Flow[LogLine].collect { case logEvent: LogEvent => logEvent }

  private lazy val csvScanner: Flow[ByteString, List[String], NotUsed] =
    CsvParsing.lineScanner(delimiter = ',', quoteChar = '"', escapeChar = ' ')
      .map(byteStringList => byteStringList.map(_.utf8String))


  private def parseLine(filePath: String)(columns: List[String]): LogLine = parser.parse(columns, Some(filePath))
}
