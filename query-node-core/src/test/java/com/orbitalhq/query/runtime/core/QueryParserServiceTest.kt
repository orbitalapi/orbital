package com.orbitalhq.query.runtime.core

import com.orbitalhq.schema.consumer.SimpleSchemaStore
import com.orbitalhq.schemas.taxi.TaxiSchema
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import org.junit.jupiter.api.Test

class QueryParserServiceTest {
   @Test
   fun `returns a parsed query`() {
      val schema = TaxiSchema.from("""
         model Film {
            id : FilmId inherits Int
            title : FilmTitle inherits String
         }
      """.trimIndent())
      val schemaStore = SimpleSchemaStore.forSchema(schema)
      val service = QueryParserService(schemaStore)

      val parsed = service.parseQuery("""find { Film[] }""")
         .block()!!
      parsed.hasErrors.shouldBeFalse()
   }

   @Test
   fun `returns a set of compilation errors when the query cant be compiled`() {
      val schema = TaxiSchema.from("""
         model Film {
            id : FilmId inherits Int
            title : FilmTitle inherits String
         }
      """.trimIndent())
      val schemaStore = SimpleSchemaStore.forSchema(schema)
      val service = QueryParserService(schemaStore)

      val parsed = service.parseQuery("""find { Movie[] }""")
         .block()!!
      parsed.hasErrors.shouldBeTrue()
   }

//   @Test
   // Waiting on taxi changes to enable this.
   fun `returns an error if a query with the same name already exists`() {
      val schema = TaxiSchema.from("""
         model Film {
            id : FilmId inherits Int
            title : FilmTitle inherits String
         }
         query FindAllFilms {
            find { Film[] }
         }
      """.trimIndent())
      val schemaStore = SimpleSchemaStore.forSchema(schema)
      val service = QueryParserService(schemaStore)

      val parsed = service.parseQuery("""
         query FindAllFilms {
            find { "Hello, world" }
         }
      """.trimIndent())
         .block()!!
      parsed.hasErrors.shouldBeTrue()
   }
}
