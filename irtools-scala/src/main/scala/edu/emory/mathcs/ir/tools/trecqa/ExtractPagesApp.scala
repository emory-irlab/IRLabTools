package edu.emory.mathcs.ir.tools.trecqa
import java.io.{File, PrintWriter}
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import edu.emory.mathcs.ir.web.{HtmlScraper, SearchResults}
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.forkjoin._

/**
  * Created by dsavenk on 12/19/16.
  */
object ExtractPagesApp {
  private implicit val ec = ExecutionContext.global

  def main(args: Array[String]): Unit = {
    val parseResult = decode[List[SearchResults]](scala.io.Source.fromFile(args(0)).mkString).right.get

    var index: AtomicInteger = new AtomicInteger(0)
    val url2Html = scala.collection.mutable.Map[String, String]()

    parseResult.foreach {
      results =>
        val allResponses = Future.sequence(results.searchResults.map {
          searchResult => HtmlScraper.getAsync(searchResult.url).map {
            html =>
              if (html.nonEmpty) {
                val filePath = args(1) + index.get().toString + ".html"
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

    val filePath = args(1) + "index.txt"
    val writer = new PrintWriter(new File(filePath))
    url2Html.foreach {
      case (url, path) =>
        writer.println(url.stripLineEnd + "\t" + path.stripLineEnd)
    }
    writer.close()
    HtmlScraper.shutdown()
  }
}
