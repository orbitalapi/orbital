package io.vyne.cockpit.core.schemas

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.vyne.models.TypeNamedInstance
import io.vyne.schema.spring.SimpleTaxiSchemaProvider
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
        val typeNamedInstance = requestResult.parseResult!!.typeNamedInstance.shouldBeInstanceOf<TypeNamedInstance>()
        typeNamedInstance.convertToRaw().shouldBe(mapOf("id" to 123, "title" to "Star Wars"))

        requestResult.newTypes.shouldHaveSize(1)
    }
}
