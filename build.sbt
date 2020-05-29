name := "spark_struc_streaming_basic_app"

version := "0.1"

scalaVersion := "2.12.11"

libraryDependencies ++= Seq(
    "org.apache.spark" %% "spark-core" % "2.4.5",
    "org.apache.spark" %% "spark-sql" % "2.4.5"
//    "com.typesafe" % "config" % "1.2.1"
)