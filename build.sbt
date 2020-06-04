name := "akka-quickstart-scala"

version := "1.0"

scalaVersion := "2.13.2"


resolvers += "Bintray" at "http://dl.bintray.com/websudos/oss-releases"

lazy val akkaVersion = "2.6.5"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  //  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
  "com.lightbend.akka" %% "akka-stream-alpakka-csv" % "2.0.0",
  "org.scalatest" %% "scalatest" % "3.1.0" % Test,
  //  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test
)
