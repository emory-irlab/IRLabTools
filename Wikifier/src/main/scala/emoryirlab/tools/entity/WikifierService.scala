package emoryirlab.tools.entity

import io.finch._
import io.finch.circe._
import io.circe.generic.auto._
import com.twitter.finagle.Http
import com.twitter.finagle.param.Stats
import com.twitter.server.TwitterServer
import com.twitter.util.Await

/**
  * Wikification RESTful service.
  */
object WikifierService extends TwitterServer {

  val api: Endpoint[Array[EntityMention]] = get("get_entities" :: param("text")) {
    text: String =>
      Ok(Wikifier.getEntityMentions(text))
  }
  val apiService = api.toService

  def main(): Unit = {
    val server = Http.server.configured(Stats(statsReceiver))
      .serve(":8081", apiService)
    onExit {
      server.close()
    }

    Await.ready(adminHttpServer)
  }
}
