name := "irtools-scala"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "dk.brics.automaton" % "automaton" % "1.11-8",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.6.0",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.6.0" classifier "models",
  "org.scala-lang.modules" %% "scala-pickling" % "0.10.1"
//  "org.apache.lucene" % "lucene-core" % "6.1.0",
//  "org.apache.lucene" % "lucene-analyzers-common" % "6.1.0",
//  "org.apache.lucene" % "lucene-queryparser" % "6.1.0"

  // In lib/ directory we need:
  // * xeger. You can get the jar from http://code.google.com/p/xeger/ and then install into local maven
  // mvn install:install-file -Dfile=xeger-1.0-SNAPSHOT.jar -DgroupId=nl.flotsam -DartifactId=xeger -Dversion=1.0-SNAPSHOT -Dpackaging=jar

  // * tagme library
)
