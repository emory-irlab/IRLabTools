package edu.emory.mathcs.ir.utils

import emoryirlab.tools.entity.EntityMention

case class AnnotatedQuestion(id: String, question: String,
                             answers: Array[String],
                             questionEntity: Array[EntityMention],
                             answerEntity: Array[EntityMention])

/**
  * Created by dsavenk on 8/4/16.
  */
object QuestionUtils {

  def filterFactoidQuestions(question: String): Boolean = {
    val lowerQuestion = question.toLowerCase
      !lowerQuestion.contains("how many") &&
      !lowerQuestion.contains("population") &&
      !lowerQuestion.contains("when") &&
      !lowerQuestion.contains("how long") &&
      !lowerQuestion.contains("what day") &&
      !lowerQuestion.contains("what date")
  }
}
