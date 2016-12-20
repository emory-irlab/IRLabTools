package edu.emory.mathcs.ir.web

case class SearchResult(rank:Int,
                        url: String,
                        title: String,
                        snippet: String)

case class SearchResults(query: String,
                         searchResults: List[SearchResult])

/**
  * Created by dsavenk on 12/19/16.
  */
trait WebSearch {
  def search(query: String, topN:Int = 50): SearchResults
}


