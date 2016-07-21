name := "irtools-scala"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "org.apache.lucene" % "lucene-core" % "6.1.0",
  "org.apache.lucene" % "lucene-analyzers-common" % "6.1.0",
  "org.apache.lucene" % "lucene-queryparser" % "6.1.0"

)
