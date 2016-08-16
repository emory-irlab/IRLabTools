package edu.emory.mathcs.ir.tools.trecqa

import java.io.{File, PrintWriter}

import edu.emory.mathcs.ir.utils.{AnnotatedQuestion, QuestionUtils}
import emoryirlab.tools.entity.{EntityMention, Wikifier}
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
    }.toList.groupBy(_._1).mapValues(_.flatMap(pattern => extendPattern(pattern._2)).toSet)
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

  def getQuestionAnswerEntityMentions(question: String, patterns: Set[String], skipFiltering: Boolean = false): (Array[EntityMention], Array[EntityMention]) = {
    val answerPatterns = patterns.mkString(", ")
    val questionWithAnswers = question + "? " + answerPatterns
    val mentions = Wikifier.getEntityMentions(questionWithAnswers)
    val (questionMentions, answerMentions) = mentions.partition(_.start < question.length)
    (questionMentions, answerMentions.filter(mention => skipFiltering || patterns.contains(questionWithAnswers.substring(mention.start, mention.end))))
  }

  def outputQuestionsJson(file: String, questionWithMentions: Seq[AnnotatedQuestion]): Unit = {
    val out = new PrintWriter(new File(file))

    out.println("[")
    for (annotatedQuestion <- questionWithMentions) { //.filter(_.answerEntity.nonEmpty)) {
      out.println("  {")
      out.println("  \"id\": \"" + annotatedQuestion.id + "\",")
      out.println("  \"utterance\": \"" + annotatedQuestion.question.replace("\"", "\\\"") + "\",")
      out.println("  \"patterns\": [")
      for (pattern <- annotatedQuestion.answers) {
        out.println("    \"" + pattern + "\", ")
      }
      out.println("  ],")
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

  def main(args: Array[String]): Unit = {
    val questions = readQuestions(args(0))
    val patterns = readPatterns(args(1))
    val prunedQuestions = readPrunedQuestions(args(2))

    val (keptQuestions: Map[String, (String, Set[String])], _) = questions
        .map(kv => kv._1 -> (kv._2, patterns(kv._1)))
      .partition {
        case (id, (question, pattern)) =>
          //filterQuestions(question, pattern)
          prunedQuestions.contains(question)
      }

    val questionWithMentions = keptQuestions map {
      case (id: String, (question: String, pattern: Set[String])) =>
        val (questionMentions, answerMentions) = getQuestionAnswerEntityMentions(question, pattern)
        AnnotatedQuestion(id, question, pattern.toArray, questionMentions, answerMentions)
    } toList

    outputQuestionsJson(args(3), questionWithMentions)
  }
}

// This app is to convert web-based answers, collected by the QuASE team to a good format.
object EntityLinkWebAnswers {

  def readQuestionAnswers(file: String): Map[String, Seq[(String, Int)]] = {
    scala.io.Source.fromFile(file).getLines()
      .map(line => line.split("\t")).toList
      .groupBy(_(0)) map {
        case (question, answers) =>
          question -> answers.sortBy(- _(3).toInt).map(answer => splitAnswerNames(answer(1))).zipWithIndex
      }
  }

  def splitAnswerNames(answer: String): String = {
    val res = new StringBuilder
    val seenWords = new collection.mutable.HashSet[String]
    for (word <- answer.split("\\s+")) {
      if (seenWords.contains(word.toLowerCase())) {
        res.append(", ")
      } else {
        res.append(" ")
      }
      res.append(word)
      seenWords.add(word.toLowerCase())
    }
    res.toString()
  }

  def main(args: Array[String]): Unit = {
    val qnas = readQuestionAnswers(args(0))
    val annotatedQna = qnas.zipWithIndex flatMap {
      case ((question, answers), index) =>
        answers map {
          case (answer, rank) =>
            val (questionEntities, answerEntities) = EntityLinkAnswers.getQuestionAnswerEntityMentions(question, Set(answer), skipFiltering = true)
            AnnotatedQuestion(index.toString + "-" + rank, question, Array(answer), questionEntities, answerEntities)
        }
    } toList

    EntityLinkAnswers.outputQuestionsJson(args(1), annotatedQna)
  }
}