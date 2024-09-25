package com.orbitalhq

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import lang.taxi.compiled

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

      it("can use a scoped variable as an input to an expression function") {
         val (vyne,stub) = testVyne("""
            model Film {
               title : Title inherits String
               minAge : Age inherits Int
            }
            service FilmsService {
               operation getFilms():Film[]
            }
            type AllowedFilms by (Film[], viewerAge:Age) -> Film[].filter( (Film) -> Film::Age > viewerAge )
               .convert(Title)
         """.trimIndent())
         stub.addResponse("getFilms", """[{"title" : "Star Wars", "minAge" : 8 }, {"title" : "Jaws" , "minAge" : 12 }]""")
       val f=  vyne.query("""given { Age = 6 } find { AllowedFilms }""")
            .typedInstances()
         f.shouldNotBeNull()
      }

      it("is possible to use argument names in expression types") {
        val (vyne) = testVyne("""
            type Name inherits String
            type UppercaseName inherits String by (name:Name) -> name.upperCase()
         """)
         vyne.query("""given { Name = 'Jimmy' } find { UppercaseName }""")
            .firstRawValue()
            .shouldBe("JIMMY")
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
