package edu.emory.mathcs.ir.tools.trecqa
import java.io.{File, PrintWriter}
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import edu.emory.mathcs.ir.web.{HtmlScraper, SearchResult, SearchResults}
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.forkjoin._

case class InputEntity(name: String, score: Float, mid: String, surface_score: Float, position: List[Int])
case class InputQnA(question: String, question_entities: Option[List[InputEntity]],
                    source: String, answer: String,
                    link:String, answer_entities: Option[List[InputEntity]])

case class NewBingSearchResult(url: String, displayUrl: String, name: String, snippet: String)

case class QuestionWithSearchResults(question: String, body: String, question_entities: List[InputEntity], source: String,
                     answer: String, answer_entities: List[InputEntity], qid:String, categories: List[String],
                     answers: List[String], search_results: List[NewBingSearchResult])

/**
  * Created by dsavenk on 12/19/16.
  */
object ExtractPagesApp {
  private implicit val ec = ExecutionContext.global

  def main(args: Array[String]): Unit = {
    //val parseResult = parseSearchResultsOld(args(0))
    val parseResult = parseSearchResults(args(0))
    downloadPages(args(1), parseResult)
  }

  private def parseSearchResults(inputPath: String): List[SearchResults] = {
    val res = decode[List[QuestionWithSearchResults]](scala.io.Source.fromFile(inputPath).mkString).right.get

    res.map(qsr => SearchResults(qsr.question, qsr.search_results.zipWithIndex.map {
      case (nbsr, rank) => SearchResult(rank, nbsr.url, nbsr.name, nbsr.snippet)
    }
    ))
  }

  private def parseSearchResultsOld(inputPath: String): List[SearchResults] = {
    decode[List[SearchResults]](scala.io.Source.fromFile(inputPath).mkString).right.get
  }

  private def downloadPages(outDir: String, parseResult: List[SearchResults]) = {
    var index: AtomicInteger = new AtomicInteger(0)
    val url2Html = scala.collection.mutable.Map[String, String]()

    parseResult.foreach {
      results =>
        val allResponses = Future.sequence(results.searchResults.map {
          searchResult =>
            HtmlScraper.getAsync(searchResult.url) map {
              html =>
                if (html.nonEmpty) {
                  val filePath = outDir + index.get().toString + ".html"
                  val writer = new PrintWriter(new File(filePath))
                  writer.println(searchResult.url)
                  writer.write(html)
                  writer.close()
                  url2Html.synchronized {
                    url2Html(searchResult.url) = filePath
                  }
                }
                index.incrementAndGet()
            }
        })
        try {
          Await.result(allResponses, Duration(3L, TimeUnit.MINUTES))
        } catch {
          case exc: Exception => System.err.println(exc.getMessage)
        }
    }

    val filePath = outDir + "index.txt"
    val writer = new PrintWriter(new File(filePath))
    url2Html.foreach {
      case (url, path) =>
        writer.println(url.stripLineEnd + "\t" + path.stripLineEnd)
    }
    writer.close()
    HtmlScraper.shutdown()
  }
}
