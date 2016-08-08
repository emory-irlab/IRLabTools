package edu.emory.mathcs.ir.tools.trecqa

import java.io.{File, PrintWriter}

import edu.emory.mathcs.ir.tools.entity.{EntityMention, TagMeWikifier}
import edu.emory.mathcs.ir.utils.{AnnotatedQuestion, QuestionUtils}
import nl.flotsam.xeger.Xeger


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

  def readPrunedQuestions(file: String): Set[String] = {
    scala.io.Source.fromFile(file).getLines().map(_.split("\t").head).toSet
  }

  def readPatterns(file: String): Map[String, Set[String]] = {
    scala.io.Source.fromFile(file).getLines().map {
      line =>
        val fields = line.split("\\s+")
        fields(0) -> fields.slice(1, fields.length).mkString(" ")
    }.toList.groupBy(_._1).mapValues(_.flatMap(pattern => extendPattern(pattern._2)).toSet).toMap
  }

  def extendPattern(pattern: String): Set[String] = {
    val res = pattern.replace(".*?", " ")
      .replace("\\s", " ")
      .replace(".*", " ")
      .replace("?:", "")
      .replace("+", "")
      .replace("*", "")
      .replace("\"", "\\\"")
    val extendedPatterns = try {
      val xeger = new Xeger(res)
      (0 to 10).map(i => xeger.generate()).toSet
    } catch {
      case e: StackOverflowError =>
        System.err.println(e)
        Set(pattern)
      case e: IllegalArgumentException =>
        System.err.println(e)
        Set(pattern)
    }

    extendedPatterns.filter(_.nonEmpty)
  }

  /**
    * Filter out questions that either ask for number (how many, population) or date (when). Also filter out questions,
    * that only contain patterns, that start with a number.
    * @param question Question text
    * @param patterns Answer patterns
    * @return True if we should keep the question
    */
  def filterQuestions(question: String, patterns: Set[String]): Boolean = {
    QuestionUtils.filterFactoidQuestions(question) && patterns.map(_.charAt(0)).exists(Character.isAlphabetic(_))
  }

  def getQuestionAnswerEntityMentions(question: String, patterns: Set[String]): (Array[EntityMention], Array[EntityMention]) = {
    val answerPatterns = patterns.mkString(", ")
    val questionWithAnswers = question + "? " + answerPatterns
    val mentions = TagMeWikifier.getEntityMentions(questionWithAnswers)
    val (questionMentions, answerMentions) = mentions.partition(_.start < question.length)
    (questionMentions, answerMentions.filter(mention => patterns.contains(questionWithAnswers.substring(mention.start, mention.end))))
  }

  def main(args: Array[String]): Unit = {
    val questions = readQuestions(args(0))
    val patterns = readPatterns(args(1))
    val prunedQuestions = readPrunedQuestions(args(2))

    val (keptQuestions, removedQuestions) = questions
        .map(kv => kv._1 -> (kv._2, patterns(kv._1)))
      .partition {
        case (id, (question, pattern)) =>
          //filterQuestions(question, pattern)
          prunedQuestions.contains(question)
      }

    val questionWithMentions = keptQuestions map {
      case (id, (question, pattern)) =>
        val (questionMentions, answerMentions) = getQuestionAnswerEntityMentions(question, pattern)
        AnnotatedQuestion(id, question, pattern.toArray, questionMentions, answerMentions)
    }


    val out = new PrintWriter(new File(args(3)))

    out.println("[")
    for (annotatedQuestion <- questionWithMentions.filter(_.answerEntity.nonEmpty)) {
      out.println("  {")
      out.println("  \"id\":" + annotatedQuestion.id + ",")
      out.println("  \"utterance\": \"" + annotatedQuestion.question.replace("\"", "\\\"") + "\",")
//      out.println("  \"patterns\": [")
//      for (pattern <- annotatedQuestion.patterns) {
//        out.println("    \"" + pattern + "\", ")
//      }
//      out.println("  ],")
//      out.println("  \"question_entities\": [")
//      for (entity <- annotatedQuestion.questionEntity) {
//        out.println("    {")
//        out.println("      \"name\": \"" + entity.entity + "\",")
//        out.println("      \"score\": " + entity.rho)
//        out.println("    }, ")
//      }
//      out.println("  ],")
      out.println("  \"result\": [")
      for (entity: String <- annotatedQuestion.answerEntity.map(_.entity).toSet) {
        out.println("    \"" + entity.replace("\"", "\\\"") + "\",")
//        out.println("    {")
//        out.println("      \"name\": \"" + entity.entity + "\",")
//        out.println("      \"start\": " + entity.start + "\",")
//        out.println("      \"end\": " + entity.end + ",")
//        out.println("      \"score\": " + entity.rho)
//        out.println("    }, ")
      }
      out.println("  ]")
      out.println("  },")
    }
    out.println("]")
    out.close()
  }
}
