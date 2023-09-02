package com.orbitalhq.queryService

import com.orbitalhq.VyneCacheConfiguration
import com.orbitalhq.cockpit.core.schemas.TaxiGraphService
import com.orbitalhq.cockpit.core.schemas.TypeLineageService
import com.orbitalhq.schema.api.SimpleSchemaProvider
import com.orbitalhq.schemas.taxi.TaxiSchema
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
      val service = TaxiGraphService(
         schemaProvider, VyneCacheConfiguration.default(),
         TypeLineageService(schemaProvider)
      )
      service.getLinksFromType("com.orbitalhq.ClientJurisdiction")
   }


}
