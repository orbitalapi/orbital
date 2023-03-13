package io.vyne.query.chat

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.shouldBe
import io.vyne.schemas.taxi.TaxiSchema

class ChatQueryParserTest : DescribeSpec({
   val apiKey = "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"

   it("should use chatGPT to parse a query") {
      val schema = TaxiSchema.from(
         """
         [[ The name of the movie ]]
         type Title inherits String

         [[ The Id of the film ]]
         type FilmId inherits Int

         [[ The review score ]]
         type Rating inherits Int

         [[ The text of a review ]]
         type ReviewText inherits String

         [[ The duration of a movie ]]
         type DurationInMinutes inherits Int

      """.trimIndent()
      )
      val parser = ChatQueryParser(apiKey)
      val sql = parser.parseToTaxiQl(schema, "Tell me how long 'Gladiator' is, and it's review score")
      TODO()

   }

   it("should parse a response from ChatGPT") {
      val json = """{
  fields: ["DurationInMinutes", "Rating"],
  conditions: [
    {
      operator: "Equals",
      left: {
        type: "Field",
        value: "Title"
      },
      right: {
        type: "Literal",
        value: "Gladiator"
      }
    }
  ]
}"""
      val query = jacksonObjectMapper()
         .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
         .readValue<ChatGptQuery>(json)
      query.shouldBe(
         ChatGptQuery(
            fields = listOf("DurationInMinutes", "Rating"),
            conditions = listOf(
               Condition(
                  Condition.Operator.Equals,
                  left = FieldCondition("Title"),
                  right = LiteralCondition("Gladiator")
               )
            )
         )
      )
   }

})
