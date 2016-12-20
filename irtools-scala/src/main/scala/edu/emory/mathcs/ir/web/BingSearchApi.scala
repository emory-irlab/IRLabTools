package edu.emory.mathcs.ir.web

import net.ettinsmoor.{Bingerator, WebResult}

/**
  * Performs the search using Bing Web Search API and returns result documents.
  */
class BingSearchApi(key: String) extends WebSearch {
  private val bingApi = new Bingerator(key)

  override def search(query: String, topN: Int = 50): SearchResults = {
    SearchResults(query, bingApi.SearchWeb(query).take(topN).zipWithIndex.map {
      case (res, rank) => this.convert(rank, res)
    }.toList)
  }

  private def convert(rank:Int, result: WebResult): SearchResult = {
    SearchResult(rank, result.url, result.title, result.description)
  }
}
