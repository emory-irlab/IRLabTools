package edu.emory.mathcs.ir.tools.clueweb

import java.nio.file.FileSystems

import io.finch._
import io.finch.circe._
import io.circe.generic.auto._
import com.twitter.finagle.Http
import com.twitter.finagle.param.Stats
import com.twitter.server.TwitterServer
import com.twitter.util.Await
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.core.KeywordAnalyzer
import org.apache.lucene.analysis.en.EnglishAnalyzer
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper
import org.apache.lucene.index.{DirectoryReader, IndexReader, MultiReader}
import org.apache.lucene.search.{IndexSearcher, TopDocs}
import org.apache.lucene.store.FSDirectory
import org.apache.lucene.util.QueryBuilder

import scala.collection.JavaConverters._

/**
  * Web service interface to Lucene web search over entity phrase index.
  */
object EntitySentenceSearchService extends TwitterServer {
  val port = flag("port", "8081", "Port to run server on")
  val indexes = flag[String]("indexes", "List of index directories to search over")

  def search(searchers: Array[IndexSearcher], queryBuilder: QueryBuilder, text: String, topN:Int=100): Array[EntityPhrase] = {
    val query = queryBuilder.createBooleanQuery("phrase", text)
    val topDocs = searchers.par.map(_.search(query, topN)).toArray
    TopDocs.merge(topN, topDocs).scoreDocs map {
      hit =>
        val doc = searchers(hit.shardIndex).doc(hit.doc)
        EntityPhrase(
          doc.get("doc"),
          doc.get("phrase"),
          doc.getValues("name").zip(doc.getValues("mid")).map(nameMid => Entity(nameMid._1, nameMid._2))
        )
    }
  }

  def main(): Unit = {
    val directories = indexes().split(",").map(path => FSDirectory.open(FileSystems.getDefault.getPath(path)))
    val searchers = directories.map(dir => DirectoryReader.open(dir)).map(new IndexSearcher(_))
    val analyzer = new PerFieldAnalyzerWrapper(
      new KeywordAnalyzer, Map[String, Analyzer]("phrase" -> new EnglishAnalyzer()).asJava)
    val queryBuilder = new QueryBuilder(analyzer)

    val api: Endpoint[Array[EntityPhrase]] = get("search" :: param("text")) {
      text: String =>
        Ok(search(searchers, queryBuilder, text))
    }
    val apiService = api.toService

    val server = Http.server.configured(Stats(statsReceiver)).serve(":" + port(), apiService)
    onExit {
      server.close()
    }

    Await.ready(adminHttpServer)
  }
}
