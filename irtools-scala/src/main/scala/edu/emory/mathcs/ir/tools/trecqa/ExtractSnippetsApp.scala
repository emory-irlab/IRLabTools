package edu.emory.mathcs.ir.tools.trecqa

import java.io.{File, PrintWriter}

import edu.emory.mathcs.ir.web.{BingSearchApi, SearchResults}
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._

import scala.io.Source

/**
  * Created by dsavenk on 12/19/16.
  */
object ExtractSnippetsApp {
  def main(args: Array[String]): Unit = {
    val keys = Array("ua4NbbaJUUabS47ZzGM2VANoW3s+EdogrHxbtRRsg1Y","qaiWmCHnf0KlqvRinysEmXYAmXrV51+K1M/YiDmPBa0","z7GKui/JBAa9bhG5VGkEeQJkdy5bH5womhPpNotNJvE","CN2AJN4aIJhnC7T3Bb1eAKS59zPSqS6xAalt3kBtq3o")
    var currentKeyIndex = 0

    val pw = new PrintWriter(new File(args(1)))
    var search = new BingSearchApi(keys(currentKeyIndex))
    Source.fromFile(args(0)).getLines.zipWithIndex.foreach {
      case (question, index) =>
        var failed = true
        while (failed) {
          try {
            val searchResults = search.search(question)
            pw.write(searchResults.asJson.spaces2)
            if (index % 100 == 0) {
              System.err.println("%d queries processed...".format(index))
            }
            failed = false
          } catch {
            case exc: java.io.IOException if exc.getMessage.contains("503") =>
              System.err.println(exc.getMessage)
              currentKeyIndex = (currentKeyIndex + 1) % keys.length
              search = new BingSearchApi(keys(currentKeyIndex))
            case exc =>
              System.err.println(exc)
              failed = false
          }
        }
    }
    pw.close()
  }
}
