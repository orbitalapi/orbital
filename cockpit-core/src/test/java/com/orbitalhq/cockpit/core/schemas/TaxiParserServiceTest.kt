package com.orbitalhq.cockpit.core.schemas

import com.orbitalhq.cockpit.core.schemas.parser.ModelParseRequest
import com.orbitalhq.cockpit.core.schemas.parser.TaxiParseRequest
import com.orbitalhq.cockpit.core.schemas.parser.TaxiParserService
import com.orbitalhq.models.TypeNamedInstance
import com.orbitalhq.schema.spring.SimpleTaxiSchemaProvider
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class TaxiParserServiceTest {

    @Test
    fun `will parse a JSON object with additional schema def`() {
        val service = TaxiParserService(
            SimpleTaxiSchemaProvider(
                """
         type FilmId inherits Int
         type Title inherits String
      """
            )
        )
        val requestResult = service.parse(
            TaxiParseRequest(
                model = ModelParseRequest(
                    model = """{ "id"  : 123, "title" : "Star Wars" }""",
                    targetType = "Film",
                    includeTypeInformation = true
                ),
                taxi = """model Film {
            |  id : FilmId
            |  title : Title
            |}
         """.trimMargin(),

                )
        )
//        val typeNamedInstance = requestResult.parseResult!!.typeNamedInstance.shouldBeInstanceOf<TypeNamedInstance>()
//        typeNamedInstance.convertToRaw().shouldBe(mapOf("id" to 123, "title" to "Star Wars"))

        requestResult.newTypes.shouldHaveSize(1)
    }

   @Test
   fun `will generate code insights for JSON`() {

   }
}
