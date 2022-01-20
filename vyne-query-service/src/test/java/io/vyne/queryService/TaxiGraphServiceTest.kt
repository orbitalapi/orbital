package io.vyne.queryService

import io.vyne.VyneCacheConfiguration
import io.vyne.schemaApi.SimpleSchemaProvider
import io.vyne.schemas.taxi.TaxiSchema
import org.apache.commons.io.IOUtils
import org.junit.Test


class TaxiGraphServiceTest {

   val taxi = """
       type Author {
         firstName : FirstName as String
         lastName : LastName as String
       }
       type Book {
         author : Author
      }
      service AuthorService {
         operation lookupByName(authorName:FirstName):Author
      }

   """.trimIndent()
   @Test
   fun when_producingTaxiGraphSchema_that_verticesAreFiltered() {
      val fullSchema = IOUtils.toString(this::class.java.getResourceAsStream("/schema.taxi"))
      val schemaProvider = SimpleSchemaProvider(TaxiSchema.from(fullSchema))
      val service = TaxiGraphService(schemaProvider, VyneCacheConfiguration.default(), TypeLineageService(schemaProvider))
      service.getLinksFromType("io.vyne.ClientJurisdiction")
   }


}
