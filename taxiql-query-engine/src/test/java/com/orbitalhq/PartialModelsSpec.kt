package com.orbitalhq

import com.orbitalhq.models.TypedObject
import com.orbitalhq.query.UnresolvedTypeInQueryException
import io.kotest.assertions.throwables.shouldThrow
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
            write operation patchFilmWithOriginal(Film):Film
            write operation patchFilmWithPartial(FilmUpdate):FilmUpdate
         }
      """.trimIndent()
         )

         stub.addResponseReturningInputs("patchFilmWithPartial")
         shouldThrow<UnresolvedTypeInQueryException> {
            vyne.query(
               """
         given { Revenue = 200 }
         call FilmsApi::patchFilmWithOriginal
      """.trimIndent()
            )
               .firstRawObject()
         }


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
   }
})
