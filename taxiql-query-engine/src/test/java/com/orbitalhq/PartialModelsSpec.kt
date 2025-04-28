package com.orbitalhq

import com.orbitalhq.models.TypedObject
import com.orbitalhq.query.UnresolvedTypeInQueryException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class PartialModelsSpec : DescribeSpec({

   describe("partial models") {
      it("can call a mutation with a partial type") {
         val (vyne, stub) = testVyne(
            """
         closed parameter model Film {
            title : Title inherits String
            revenue : Revenue inherits Int
         }
        @com.orbitalhq.models.OmitNulls
         partial model FilmUpdate from Film
         service FilmsApi {
            operation getFilm():Film
            write operation patchFilmWithOriginal(Film):Film
            write operation patchFilmWithPartial(FilmUpdate):FilmUpdate
         }
      """.trimIndent()
         )

         stub.addResponseReturningInputs("patchFilmWithPartial")
            vyne.query(
               """
         given { Revenue = 200 }
         call FilmsApi::patchFilmWithOriginal
      """.trimIndent()
            ).shouldEmitError()


         vyne.query(
            """
         given { Revenue = 200 }
         call FilmsApi::patchFilmWithPartial
      """.trimIndent()
         )
            .firstRawObject()

         val callInput = stub.calls["patchFilmWithPartial"].single().single()
         callInput.shouldBeInstanceOf<TypedObject>()
            .toRawObject()
            .shouldBe(mapOf("revenue" to 200))
      }

       it("can call a mutation with a partial type having  object property") {
           val (vyne, stub) = testVyne(
               """
         closed parameter model Film {
             info : {
             title: Title inherits String
             director: Director inherits String
            }
            revenue : Revenue inherits Int
         }
        @com.orbitalhq.models.OmitNulls
         partial model FilmUpdate from Film
         service FilmsApi {
            operation getFilm():Film
            write operation patchFilmWithPartial(FilmUpdate):FilmUpdate
         }
      """.trimIndent()
           )

           stub.addResponseReturningInputs("patchFilmWithPartial")
           stub.addResponse("getFilm", """
               {
                  "revenue": 200,
                  "info": {

                  }
               }
           """.trimIndent())
           vyne.query(
               """
         find { Film }
         call FilmsApi::patchFilmWithPartial
      """.trimIndent()
           )
               .firstRawObject()

           val callInput = stub.calls["patchFilmWithPartial"].single().single()
           callInput.shouldBeInstanceOf<TypedObject>()
               .toRawObject()
               .shouldBe(mapOf("revenue" to 200))
       }

       it("can call a mutation with a partial type having a nested object property") {
           val (vyne, stub) = testVyne(
               """
         closed parameter model Film {
             info : {
                 title: Title inherits String
                 director: {
                   name: DirectorName inherits String
                   country: Country inherits String
                 }
            }
            revenue : Revenue inherits Int
         }
        @com.orbitalhq.models.OmitNulls
         partial model FilmUpdate from Film
         service FilmsApi {
            operation getFilm():Film
            write operation patchFilmWithPartial(FilmUpdate):FilmUpdate
         }
      """.trimIndent()
           )

           stub.addResponseReturningInputs("patchFilmWithPartial")
           stub.addResponse("getFilm", """
               {
                  "revenue": 200,
                  "info": {
                     "title": "The Wild Bunch",
                     "director": {}

                  }
               }
           """.trimIndent())
           vyne.query(
               """
         find { Film }
         call FilmsApi::patchFilmWithPartial
      """.trimIndent()
           )
               .firstRawObject()

           val callInput = stub.calls["patchFilmWithPartial"].single().single()
           callInput.shouldBeInstanceOf<TypedObject>()
               .toRawObject()
               .shouldBe(mapOf("revenue" to 200, "info" to mapOf("title" to "The Wild Bunch")))
       }
   }

   it("can populate nested attributes of partials where original type was closed") {
      val (vyne, stub) = testVyne(
         """
         closed model Actor {
            name : Name inherits String
            salary : Salary inherits Int
         }
         closed parameter model Film {
            title : Title inherits String
            cast : Actor[]
         }
        @com.orbitalhq.models.OmitNulls
         partial model FilmUpdate from Film
         service FilmsApi {
            write operation patchFilmWithPartial(FilmUpdate):FilmUpdate
         }
      """.trimIndent()
      )

      stub.addResponseReturningInputs("patchFilmWithPartial")
      vyne.query(
         """
         given { actorName:Name = 'Jimmy'  }
         find { data: listOf(Actor${'$'}Partial) }
         call FilmsApi::patchFilmWithPartial
      """.trimIndent()
      )
         .firstRawObject()

      val callInput = stub.calls["patchFilmWithPartial"].single().single()
      callInput.shouldBeInstanceOf<TypedObject>()
         .toRawObject()
         .shouldBe(mapOf("cast" to listOf(
            mapOf("name" to "Jimmy"))))
   }
})
