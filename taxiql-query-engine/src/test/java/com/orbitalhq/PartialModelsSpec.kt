package com.orbitalhq

import com.orbitalhq.models.TypedObject
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

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
            write operation patchFilm(FilmUpdate):FilmUpdate
         }
      """.trimIndent()
         )

         stub.addResponseReturningInputs("patchFilm")

         vyne.query(
            """
         given { Revenue = 200 }
         call FilmsApi::patchFilm
      """.trimIndent()
         )
            .firstRawObject()

         val callInput = stub.calls["patchFilm"].single().single()
         callInput.shouldBeInstanceOf<TypedObject>()
            .toRawObject()
            .shouldBe(mapOf("revenue" to 200))
      }
   }
   })
