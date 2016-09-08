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

case class Entity(name: String, mid: String)

case class EntityPhrase(doc: String, phrase: String, entities: Array[Entity], score: Option[Float])

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
        val fields = line.split("\t")
        if (fields.size > 2) {
          val doc = fields(0)
          val phrase = fields(1)
          val entities = fields.slice(2, fields.size).grouped(2).map(e => Entity(e(0), e(1).substring(1).replace("/", "."))).toArray
          indexPhrase(writer, EntityPhrase(doc, phrase, entities, None))
        }
        line = scala.io.StdIn.readLine()
      }
    } finally {
      writer.commit()
      writer.close()
    }
  }

  def indexPhrase(indexWriter: IndexWriter, entityPhrase: EntityPhrase): Unit = {
    phraseCounter += 1
    val doc = new Document()
    doc.add(new TextField("phrase", entityPhrase.phrase, Field.Store.YES))
    doc.add(new Field("doc", entityPhrase.doc, StoredField.TYPE))
    for (Entity(name, mid) <- entityPhrase.entities) {
      doc.add(new Field("mid", mid, StringField.TYPE_STORED))
      doc.add(new TextField("name", name, Field.Store.YES))
    }

    indexWriter.addDocument(doc)

    if (phraseCounter % 1000 == 0)
      System.err.println(s"$phraseCounter phrases indexed")
  }
}
