package edu.emory.mathcs.ir.tools

import java.nio.file.Paths

import org.apache.lucene.analysis.Analyzer

import scala.collection.JavaConverters._
import org.apache.lucene.analysis.core.KeywordAnalyzer
import org.apache.lucene.analysis.en.EnglishAnalyzer
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper
import org.apache.lucene.document._
import org.apache.lucene.index.{IndexWriter, IndexWriterConfig}
import org.apache.lucene.store.FSDirectory

/**
  * Created by dsavenk on 7/21/16.
  */
object IndexWikianswersApp {
  val englishAnalyzer = new EnglishAnalyzer
  val analyzer = new PerFieldAnalyzerWrapper(
    new KeywordAnalyzer,
    Map[String, Analyzer]("question" -> englishAnalyzer,
      "answer" -> englishAnalyzer).asJava)
  val indexConfig = new IndexWriterConfig(analyzer)
  var clusterCounter = 0
  var questionCounter = 0

  def main(args: Array[String]): Unit = {
    val indexDirectory = FSDirectory.open(Paths.get(args(0)))
    val writer = new IndexWriter(indexDirectory, indexConfig)

    try {
      var line: String = scala.io.StdIn.readLine()

      while (line != null) {
        val questionCluster = line.split("\t").map(_.split(":"))
        indexCluster(writer, questionCluster)
        line = scala.io.StdIn.readLine()
      }
    } finally {
      writer.commit()
      writer.close()
    }
  }

  def indexCluster(indexWriter: IndexWriter, questionCluster: Array[Array[String]]): Unit = {
    clusterCounter += 1
    val answers = questionCluster.filter(_(0) == "a").map(_(1))

    questionCluster.filter(_(0) == "q").map(_(1)).foreach { question =>
      questionCounter += 1
      val doc = new Document()
      doc.add(new TextField("question", question, Field.Store.YES))
      doc.add(new Field("cluster", clusterCounter.toString, StoredField.TYPE))
      doc.add(new Field("id", questionCounter.toString, StoredField.TYPE))
      answers.foreach(answer => doc.add(new TextField("answer", answer, Field.Store.YES)))
      indexWriter.addDocument(doc)
    }
    if (clusterCounter % 1000 == 0)
        System.err.println(s"$clusterCounter clusters indexed")
  }
}
