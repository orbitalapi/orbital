package io.vyne.queryService.schemas

import io.vyne.models.json.Jackson
import io.vyne.query.TaxiJacksonModule
import io.vyne.query.VyneJacksonModule
import io.vyne.schemas.taxi.TaxiSchema
import org.junit.Test

class SchemaSerializationTest {

   @Test
   fun quick() {
      val schema = TaxiSchema.from("""

         namespace io.vyne.demo {

         type LastName inherits String
         enum Country {
            [[ Home sweet home ]]
            NZ("New Zealand"),
            [[ Ozzie ]]
            AUS("Australia"),
            [[ The queen lives here ]]
            UK("United Kingdom")
         }
         model Address {
            firstLine : FirstLine inherits String
            lastLine : LastLine inherits String
            region : {
               country : Country
               postCode : PostCode inherits String
            }
         }
         model Person {
            firstName : FirstName inherits String
            lastName : LastName?
            address : Address
         }
         }
      """.trimIndent())
      val json = Jackson.defaultObjectMapper
         .registerModule(VyneJacksonModule())
         .registerModule(TaxiJacksonModule())
         .writerWithDefaultPrettyPrinter().writeValueAsString(schema)
      TODO()
   }
}
