package edu.emory.mathcs.ir.tools.clueweb

import java.nio.file.Paths

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.core.KeywordAnalyzer
import org.apache.lucene.analysis.en.EnglishAnalyzer
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper
import org.apache.lucene.document._
import org.apache.lucene.index.{IndexWriter, IndexWriterConfig}
import org.apache.lucene.store.FSDirectory

import scala.collection.JavaConverters._

case class EntityPairPhrase(doc: String, mid1: String, name1: String,
                            mid2: String, name2: String, phrase: String)

/**
  * Created by dsavenk on 7/21/16.
  */
object EntityPairPhrasesIndexerApp {
  val englishAnalyzer = new EnglishAnalyzer
  val analyzer = new PerFieldAnalyzerWrapper(
    new KeywordAnalyzer,
    Map[String, Analyzer]("phrase" -> englishAnalyzer,
      "answer" -> englishAnalyzer).asJava)
  val indexConfig = new IndexWriterConfig(analyzer)
  var phraseCounter = 0

  def main(args: Array[String]): Unit = {
    val indexDirectory = FSDirectory.open(Paths.get(args(0)))
    val writer = new IndexWriter(indexDirectory, indexConfig)

    try {
      var line: String = scala.io.StdIn.readLine()

      while (line != null) {
        val Array(doc, mid1, name1, mid2, name2, phrase) = line.split("\t")

        indexPhrase(writer, EntityPairPhrase(doc, mid1, name1, mid2, name2, phrase))
        line = scala.io.StdIn.readLine()
      }
    } finally {
      writer.commit()
      writer.close()
    }
  }

  def indexPhrase(indexWriter: IndexWriter, phrase: EntityPairPhrase): Unit = {
    phraseCounter += 1
    val doc = new Document()
    doc.add(new TextField("phrase", phrase.phrase, Field.Store.YES))
    doc.add(new Field("doc", phrase.doc, StoredField.TYPE))
    doc.add(new Field("mid", phrase.mid1, StringField.TYPE_STORED))
    doc.add(new Field("name", phrase.name1, StringField.TYPE_STORED))
    doc.add(new Field("mid", phrase.mid2, StringField.TYPE_STORED))
    doc.add(new Field("name", phrase.name2, StringField.TYPE_STORED))
    indexWriter.addDocument(doc)

    if (phraseCounter % 1000 == 0)
      System.err.println(s"$phraseCounter phrases indexed")
  }
}
