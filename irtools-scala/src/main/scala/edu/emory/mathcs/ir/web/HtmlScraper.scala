package edu.emory.mathcs.ir.web

import java.util.concurrent.TimeUnit

import dispatch._
import Defaults._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * Created by dsavenk on 12/19/16.
  */
object HtmlScraper {
  private val http = Http.configure(_ setFollowRedirect true)

  def get(requestUrl: String): String = {
    try {
      val resp = http(url(requestUrl) OK as.String)
      Await.result(resp, Duration(3L, TimeUnit.SECONDS))
    } catch {
      case exc: Exception => System.err.println(exc.getMessage)
        ""
    }
  }

  def getAsync(requestUrl: String): Future[String] = {
    try {
      http(url(requestUrl) OK as.String) recover {
        case exc => System.err.println(exc.getMessage)
          ""
      }
    } catch {
      case exc: Exception => System.err.println(exc.getMessage)
        Future("")
    }
  }

  def shutdown(): Unit = {
    http.shutdown()
    Http.shutdown()
  }
}
