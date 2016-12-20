package edu.emory.mathcs.ir.tools.trecqa

import java.io.PrintWriter

import scala.collection.JavaConverters._
import edu.emory.mathcs.ir.utils.Stopwords
import edu.emory.mathcs.ir.web.{ContentExtractor, SearchResult, SearchResults}
import edu.stanford.nlp.simple.Document
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

case class QuerySentences(query: String, sentences: List[Sentence])
case class Sentence(documentRank: Int,
                    url: String,
                    text: String,
                    lemmas: List[String],
                    querySimilarity: Double)

/**
  * Created by dsavenk on 12/20/16.
  */
object ExtractRelevantSentencesApp {

  def readPageHtml(path: String): String = {
    val encodings = Array("utf-8", "ISO-8859-1", "windows-1252")
    encodings.view.map {
      encoding =>
        try {
          scala.io.Source.fromFile(path, encoding).getLines().zipWithIndex.toList.filter(_._2 > 0).map(_._1).mkString
        } catch {
          case exc: Exception =>
            System.err.println(exc.getMessage)
            ""
        }
    }.collectFirst {
      case html if html.nonEmpty => html
    }.getOrElse("")
  }

  def getSentences(text: String): Seq[edu.stanford.nlp.simple.Sentence] = {
    new Document(text).sentences().asScala
  }

  def getLemmas(text: String): Seq[String] = {
    getSentences(text).flatMap(getLemmas)
  }

  def getLemmas(sent: edu.stanford.nlp.simple.Sentence): Seq[String] = {
    sent.lemmas.asScala.map(_.toLowerCase()).filter(
      lemma =>
        lemma.exists(Character.isLetterOrDigit) &&
          !(lemma.startsWith("-") && lemma.endsWith("-")) &&
          Stopwords.not(lemma)
    )
  }

  def cosineSimilarity(queryWordVector: Map[String, Int], sentenceWordVector: Map[String, Int]): Double = {
    def getL2Norm(x: Iterable[Int]) = {
      math.sqrt(x.map(v => v * v).sum)
    }

    val dotProduct = queryWordVector.filter {
      case (word, count) => sentenceWordVector.contains(word)
    }.map {
      case (word, count) => 1.0 * sentenceWordVector(word) * count
    }.sum

    dotProduct
  }

  def containsQuestion(html: String, question: String): Boolean = {
    val text = html.replaceAll("""\p{Punct}""", " ").replaceAll("\\s+", " ").toLowerCase.trim
    val questionText = question.replaceAll("""\p{Punct}""", " ").replaceAll("\\s+", " ").toLowerCase.trim
    text.contains(questionText)
  }

  def getQueryRelevantSentences(urlPathMap: Map[String, String],
                                searchResults: SearchResults,
                                minSentenceLength: Int = 30,
                                maxSentenceLength: Int = 400,
                                topN: Int = 100): QuerySentences = {
    val snippetLemmas = searchResults.searchResults.map(res => s"${res.title}. ${res.snippet}").flatMap(getLemmas)
    val queryLemmas = getLemmas(searchResults.query)
    val queryWordVector = (queryLemmas ++ snippetLemmas).groupBy(identity).mapValues(_.size)

    val pageSentences = searchResults.searchResults.filter(res => urlPathMap.contains(res.url))
      .flatMap { res =>
        val html = readPageHtml(urlPathMap(res.url))

        // Filtering out pages about TREC or that mention the question text itself.
        if (!containsQuestion(html, searchResults.query) && !html.toLowerCase.contains("trec")) {
          ContentExtractor(html, contentMinLength = minSentenceLength).flatMap(getSentences).filter(_.length <= maxSentenceLength).map {
            sent =>
              val lemmas = getLemmas(sent)
              Sentence(res.rank, res.url, sent.text.replaceAll("\\s+", " "), lemmas.toList,
                cosineSimilarity(queryWordVector, lemmas.groupBy(identity).mapValues(_.size)))
          }
        } else {
          Nil
        }
      }
      .filter(_.querySimilarity > 0)
      .sortBy(- _.querySimilarity)
      .take(topN)
    QuerySentences(searchResults.query, pageSentences)
  }

  def main(args: Array[String]): Unit = {
    val searchResultsFile = args(0)
    val pagesIndexFile = args(1)
    val outFile = args(2)

    val urlPathMap = scala.io.Source.fromFile(pagesIndexFile).getLines().map(_.trim.split("\t")).map {
      case Array(url, path) => url -> path
    }.toMap

    val searchResults = decode[List[SearchResults]](scala.io.Source.fromFile(args(0)).mkString).right.get
    val searchSentences = searchResults.map(results => getQueryRelevantSentences(urlPathMap, results)).asJson

    val writer = new PrintWriter(outFile)
    writer.write(searchSentences.asJson.spaces2)
    writer.close()
  }

}
