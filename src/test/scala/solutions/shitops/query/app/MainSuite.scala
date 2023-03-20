package solutions.shitops.query.app

import io.circe.literal.JsonStringContext
import org.scalatest.funsuite.AnyFunSuite

class MainSuite extends AnyFunSuite {
  test("question") {
    val expected = json"""{"title": "What does the fox say?"}"""
    val actual = Main.questionJson

    assert(expected == actual)
  }

  test("questions") {
    val expected =
      json"""[
             {"title": "What does the fox say?"},
             {"title": "How does the horse move?"},
             {"title": "How do you serialize a list of case classes?"}]"""
    val actual = Main.questionsJson
    assert(expected == actual)
  }
}
