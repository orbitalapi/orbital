package com.orbitalhq

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class ExpressionTypeSpec : DescribeSpec({

   describe("Expression types") {
      it("evaluates when clause on an expression type correctly") {
         val (vyne) = testVyne(
            """
      type CustomerType inherits String
      type AccountType inherits String by when(lowerCase(CustomerType)) {
           'retail'  -> 'Personal'
           'sme' -> 'Personal'
           else -> 'Business'
      }
   """
         )
         vyne.query("""
         given { CustomerType = 'Retail' }
         find { AccountType }
      """.trimIndent())
            .firstRawValue().shouldBe("Personal")
      }

      it("can filter an array fetched from a service") {
         val (vyne,stub) = testVyne("""
            model FilmRating {
               rating : RatingCode inherits String
               meaning : RatingName inherits String
            }
            type GenreId inherits String

            service FilmDataService {
              operation getFilmRatings():FilmRating[]
            }

            type FilmSubgenre inherits String by (FilmRating[], GenreId) ->  FilmRating[]
                    .filter( (RatingCode, RatingName) -> RatingCode == GenreId )
         """.trimIndent())
         stub.addResponse("getFilmRatings", """[
  {
    "rating": "G",
    "meaning": "General Audience"
  },
  {
    "rating": "PG",
    "meaning": "Parental Guidance Suggested"
  },
  {
    "rating": "PG-13",
    "meaning": "Parents Strongly Cautioned"
  },
  {
    "rating": "R",
    "meaning": "Restricted"
  },
  {
    "rating": "NC-17",
    "meaning": "Adults Only"
  }
]""")
         val queryResult = vyne.query("""given { GenreId = 'PG-13' } find { FilmSubgenre }""")
            .firstRawValue()
         queryResult.shouldNotBeNull()
      }
   }
})
