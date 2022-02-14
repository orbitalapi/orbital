package io.vyne.models.json

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.winterbe.expekt.should
import io.vyne.models.Provided
import io.vyne.models.TypedInstance
import io.vyne.models.TypedObject
import io.vyne.models.TypedValue
import io.vyne.schemas.taxi.TaxiSchema
import org.junit.Test


class JsonParsedStructureTest {
   @Test
   fun `when a model with accessor parsed from a json parsed structure`() {
      val schema = TaxiSchema.from("""
         model EsgData {
            isin : String by column("ISIN")
            esgScore : Decimal by column("Average Score")
         }
      """.trimIndent()

      )
      val jsonParsedStructure = JsonParsedStructure.from("""
         {
            "isin": "IT1231232",
            "esgScore": 1.232
         }
      """.trimIndent(), jacksonObjectMapper())
      val typedObject = TypedInstance.from(schema.type("EsgData"), jsonParsedStructure, schema, source = Provided) as TypedObject
      val isinValue = typedObject["isin"] as TypedValue
      isinValue.value.should.equal("IT1231232")
   }
}
