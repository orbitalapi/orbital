package com.orbitalhq.query.chat

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import com.orbitalhq.query.VyneQlGrammar
import com.orbitalhq.schemas.taxi.TaxiSchema

class TaxiQlGeneratorTest : DescribeSpec({
   val schema = TaxiSchema.from(
      """
          ${VyneQlGrammar.QUERY_TYPE_TAXI}

         namespace films {

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

      model Film {
         id : FilmId
         title : Title
         rating : Rating
      }
      service FilmsDb {
         table films : Film[]
      }

      }
      """.trimIndent()
   )
   it("should generate TaxiQL") {
      val query = ChatGptQuery(
         structuredQuery = StructuredQuery(
            fields = listOf("films.DurationInMinutes", "films.Rating"),
            conditions = listOf(
               Condition(
                  Condition.Operator.Equals,
                  left = FieldCondition("films.Title"),
                  right = LiteralCondition("Gladiator")
               )
            )
         )
      )
      val taxi = TaxiQlGenerator.convertToTaxi(query, schema)
      taxi.shouldBe(
         """find { Film[]( films.Title == "Gladiator" ) }
as {
   DurationInMinutes : films.DurationInMinutes
   Rating : films.Rating
}[]"""
      )
   }


})
