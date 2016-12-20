name := "irtools-scala"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.github.finagle" %% "finch-core" % "0.11.1",
  "com.github.finagle" %% "finch-circe" % "0.11.1",
  "com.google.protobuf" % "protobuf-java" % "3.0.0",
  "com.twitter" % "twitter-server_2.11" % "1.25.0",
  "commons-io" % "commons-io" % "2.5",
  "dk.brics.automaton" % "automaton" % "1.11-8",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.6.0",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.6.0" classifier "models",
  "io.circe" %% "circe-core" % "0.7.0-M1",
  "io.circe" %% "circe-generic" % "0.7.0-M1",
  "io.circe" %% "circe-parser" % "0.7.0-M1",
  "net.ettinsmoor" %% "bingerator" % "0.2.4",
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.3",
  "org.apache.thrift" % "libthrift" % "0.9.3" pomOnly(),
  "org.apache.tika" % "tika-core" % "1.14",
  "org.apache.tika" % "tika-parsers" % "1.14",
  "org.jwat" % "jwat-warc" % "1.0.4",
  "org.scala-lang.modules" %% "scala-pickling" % "0.11.0-M2",
  "org.typelevel" % "cats-core_2.11" % "0.8.1",
  "org.apache.lucene" % "lucene-core" % "6.3.0",
  "org.apache.lucene" % "lucene-analyzers-common" % "6.3.0",
  "org.apache.lucene" % "lucene-queryparser" % "6.3.0"

  // In lib/ directory we need:
  // * xeger. You can get the jar from http://code.google.com/p/xeger/ and then install into local maven
  // mvn install:install-file -Dfile=xeger-1.0-SNAPSHOT.jar -DgroupId=nl.flotsam -DartifactId=xeger -Dversion=1.0-SNAPSHOT -Dpackaging=jar

  // Wikifier project from IRTools
)