package edu.emory.mathcs.ir.tools.webscope

import java.io.{File, PrintWriter}

import edu.emory.mathcs.ir.tools.entity.{EntityMention, TagMeWikifier}
import edu.emory.mathcs.ir.utils.{AnnotatedQuestion, QuestionUtils}

/**
  * Created by dsavenk on 8/4/16.
  */
object EntityLinkAnswers {

  def readQuestionAnswers(file: String): Set[(String, String, String)] = {
    scala.io.Source.fromFile(file).getLines().map {
      line =>
        val fields = line.split("\t")
        (fields(5), fields(2), fields(3))
    }.toSet
  }

  def getQuestionAnswerEntityMentions(question: String, answer: String): (Array[EntityMention], Array[EntityMention]) = {
    val questionWithAnswers = question + "? " + answer
    val mentions = TagMeWikifier.getEntityMentions(questionWithAnswers)
    mentions.partition(_.start < question.length)
  }

  def main(args: Array[String]): Unit = {
    val (qna, filtered) = readQuestionAnswers(args(0))
      .partition(qna => QuestionUtils.filterFactoidQuestions(qna._2) && !qna._2.contains("__"))

    val annotatedQna = qna map {
      case (id, question, answer) =>
        val (questionMentions, answerMentions) = getQuestionAnswerEntityMentions(question, answer)
        AnnotatedQuestion(id, question, Array(answer), questionMentions, answerMentions)
    }

    val out = new PrintWriter(new File(args(1)))

    out.println("[")
    for (annotatedQuestion <- annotatedQna.filter(_.answerEntity.nonEmpty)) {
      out.println("  {")
      out.println("  \"id\":" + annotatedQuestion.id + ",")
      out.println("  \"utterance\": \"" + annotatedQuestion.question.replace("\"", "\\\"") + "\",")
      out.println("  \"question_entities\": [")
      for (entity <- annotatedQuestion.questionEntity) {
        out.println("    {")
        out.println("      \"name\": \"" + entity.entity + "\",")
        out.println("      \"score\": " + entity.rho)
        out.println("    }, ")
      }
      out.println("  ],")
      out.println("  \"result\": [")
//      for (entity: String <- annotatedQuestion.answerEntity.map(_.entity).toSet) {
//        out.println("    \"" + entity.replace("\"", "\\\"") + "\",")
      for (entity <- annotatedQuestion.answerEntity) {
                out.println("    {")
                out.println("      \"name\": \"" + entity.entity + "\",")
                out.println("      \"start\": " + entity.start + "\",")
                out.println("      \"end\": " + entity.end + ",")
                out.println("      \"score\": " + entity.rho)
                out.println("    }, ")
      }
      out.println("  ]")
      out.println("  },")
    }
    out.println("]")
    out.close()
  }
}
