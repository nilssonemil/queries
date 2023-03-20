package solutions.shitops.query.app

import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._

case class Question(title: String)

object Main {
  val question: Question = Question("What does the fox say?")
  val questions: List[Question] = List(
    question,
    Question("How does the horse move?"),
    Question("How do you serialize a list of case classes?"),
  )
  val questionJson: Json = question.asJson
  val questionsJson: Json = questions.asJson

}
