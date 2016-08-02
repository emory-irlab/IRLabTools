package edu.emory.mathcs.ir.tools.trecqa

import edu.emory.mathcs.ir.tools.entity.{EntityMention, TagMeWikifier}
import nl.flotsam.xeger.Xeger

case class Question(id: String, text: String)


/**
  * Created by dsavenk on 8/2/16.
  */
object EntityLinkAnswers {

  def readQuestions(file: String): Map[String, String] = {
    scala.io.Source.fromFile(file).getLines().map {
      line =>
        val fields = line.split("\\s+")
        fields(0) -> fields.slice(1, fields.length).mkString(" ")
    }.toMap
  }

  def readPatterns(file: String): Map[String, List[String]] = {
    scala.io.Source.fromFile(file).getLines().map {
      line =>
        val fields = line.split("\\s+")
        fields(0) -> fields.slice(1, fields.length).mkString(" ")
    }.toList.groupBy(_._1).mapValues(_.flatMap(pattern => extendPattern(pattern._2)))
  }

  def extendPattern(pattern: String): Set[String] = {
    try {
      val xeger = new Xeger(pattern)
      (0 to 10).map(i => xeger.generate()).toSet
    } catch {
      case e: StackOverflowError =>
        System.err.println(e)
        Set(pattern)
      case e: IllegalArgumentException =>
        System.err.println(e)
        Set(pattern)
    }
  }

  def getQuestionAnswerEntityMentions(trainPatterns: Map[String, Set[String]], id: String, question: String): (Array[EntityMention], Array[EntityMention]) = {
    val answerPatterns = trainPatterns(id).mkString("\t")
    val questionWithAnswers = question + "\t" + answerPatterns
    val mentions = TagMeWikifier.getEntityMentions(questionWithAnswers)
    val (questionMentions, answerMentions) = mentions.partition(_.start < question.length)
    (questionMentions, answerMentions)
  }

  def main(args: Array[String]): Unit = {
    val trainQuestions = readQuestions(args(0))
    val testQuestions = readQuestions(args(1))
    val trainPatterns = readPatterns(args(2)).mapValues(_.toSet)
    val testPatterns = (readPatterns(args(3)).toSeq ++ readPatterns(args(4)).toSeq).groupBy(_._1)
      .mapValues(_.flatMap(_._2).toList).mapValues(_.toSet)

    val trainEntityMentions = trainQuestions.map {
      case (id, question) =>
        id -> getQuestionAnswerEntityMentions(trainPatterns, id, question)
    }

    val testEntityMentions = testQuestions.map {
      case (id, question) =>
        id -> getQuestionAnswerEntityMentions(testPatterns, id, question)
    }

    println(trainEntityMentions)
  }

}
