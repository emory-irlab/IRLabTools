package edu.emory.mathcs.ir.utils

/**
  * Created by dsavenk on 5/27/16.
  */
object Stopwords {
  val stopwords : Set[String] = scala.io.Source.fromInputStream(getClass.getResourceAsStream("/data/stopwords.txt")).getLines.toSet

  def has(word: String): Boolean = stopwords.contains(word.toLowerCase)

  def not(word: String): Boolean = !has(word)
}