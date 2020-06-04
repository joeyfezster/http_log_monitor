package com.joeybaruch

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import com.joeybaruch.importer.CsvImporter
import com.joeybaruch.repository.ReadingRepository


object Importer extends App {

  implicit val system = ActorSystem("akka-streams-in-practice")

  private val config = ConfigFactory.load()
  private val readingRepository = new ReadingRepository

  import system.dispatcher

  new CsvImporter(config, readingRepository).importFromFiles
    .onComplete { _ =>
      readingRepository.shuthdown
      system.terminate()
    }
}
