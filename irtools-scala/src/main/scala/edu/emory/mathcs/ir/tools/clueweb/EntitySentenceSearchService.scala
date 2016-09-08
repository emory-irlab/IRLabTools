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
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser
import org.apache.lucene.search.{BooleanClause, BooleanQuery, IndexSearcher, TopDocs}
import org.apache.lucene.store.FSDirectory
import org.apache.lucene.util.QueryBuilder

import scala.collection.JavaConverters._

/**
  * Web service interface to Lucene web search over entity phrase index.
  */
object EntitySentenceSearchService extends TwitterServer {
  val port = flag("port", "8081", "Port to run server on")
  val indexes = flag[String]("indexes", "List of index directories to search over")

  val DEFAULT_TOPN = 100

  def search(searchers: Array[IndexSearcher],
             queryParser: MultiFieldQueryParser,
             text: String,
             mids: Seq[String],
             topN: Int): Array[EntityPhrase] = {
    val queryBuilder = new BooleanQuery.Builder().add(queryParser.createBooleanQuery("phrase", text), BooleanClause.Occur.SHOULD)
    mids.map(queryParser.createBooleanQuery("mid", _)).foreach(queryBuilder.add(_, BooleanClause.Occur.MUST))
    val topDocs = searchers.par.map(_.search(queryBuilder.build(), topN)).toArray
    TopDocs.merge(topN, topDocs).scoreDocs map {
      hit =>
        val doc = searchers(hit.shardIndex).doc(hit.doc)
        EntityPhrase(
          doc.get("doc"),
          doc.get("phrase"),
          doc.getValues("name").zip(doc.getValues("mid")).map(nameMid => Entity(nameMid._1, nameMid._2)),
          Some(hit.score)
        )
    }
  }

  def main(): Unit = {
    val directories = indexes().split(",").map(path => FSDirectory.open(FileSystems.getDefault.getPath(path)))
    val searchers = directories.map(dir => DirectoryReader.open(dir)).map(new IndexSearcher(_))
    val analyzer = new PerFieldAnalyzerWrapper(
      new KeywordAnalyzer, Map[String, Analyzer]("phrase" -> new EnglishAnalyzer()).asJava)
    val queryParser = new MultiFieldQueryParser(Array("phrase", "mid"), analyzer)

    val api: Endpoint[Array[EntityPhrase]] =
      get("search" :: param("phrase") :: params("mid") :: paramOption("topn").as[Int]) {
        (text: String, mids: Seq[String], topN: Option[Int]) =>
          Ok(search(searchers, queryParser, text, mids, topN.getOrElse(DEFAULT_TOPN)))
      }
    val apiService = api.toService

    val server = Http.server.configured(Stats(statsReceiver)).serve(":" + port(), apiService)
    onExit {
      server.close()
    }

    Await.ready(adminHttpServer)
  }
}
