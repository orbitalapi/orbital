package com.orbitalhq.schemas

import com.orbitalhq.query.VyneQlGrammar
import com.orbitalhq.schemas.taxi.TaxiSchema
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import lang.taxi.query.TaxiQlQuery
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ServiceFilteredSchemaTest {
   val schema = TaxiSchema.fromStrings(
      VyneQlGrammar.QUERY_TYPE_TAXI,
      """
      model Film {}
      service FilmsService {
         operation listAllFilms(): Film[]
         operation listAllBlockbusters(): Film[]

         table films : Film[]

         stream newReleases : Stream<Film>
      }

      service FilmsDb {
         table films : Film[]

         stream newReleases : Stream<Film>
       }

      service AnotherService {
         operation listNetflixFilms():Film[]
         table films : Film[]

         stream newReleases : Stream<Film>
      }
   """.trimIndent())

   @Test
   fun `only includes specified services when query declares 'using'`() {
      val (_,_,querySchema) = schema.parseQuery("""find { Film[] } using { FilmsService }""")
      querySchema.services.shouldHaveSize(1)
      querySchema.operations.shouldHaveSize(2)
      querySchema.remoteOperations.shouldHaveSize(5) // table exposes 2 remote operations

      querySchema.tableOperations.shouldHaveSize(1)
      querySchema.streamOperations.shouldHaveSize(1)
   }

   @Test
   fun `only includes specified operations when query declares 'using'`() {
      val (_,_,querySchema) = schema.parseQuery("""find { Film[] } using { FilmsService::listAllFilms }""")
      querySchema.services.shouldHaveSize(1)
      querySchema.operations.shouldHaveSize(1)
      querySchema.remoteOperations.shouldHaveSize(1) // table exposes 2 remote operations

      querySchema.tableOperations.shouldHaveSize(0)
      querySchema.streamOperations.shouldHaveSize(0)
   }

   @Test
   fun `includes everything except those excluded when query declares 'excluded' services`() {
      val (_,_,querySchema) = schema.parseQuery("""find { Film[] } excluding { FilmsService }""")
      querySchema.services.shouldHaveSize(2)
      querySchema.operations.shouldHaveSize(1)
      querySchema.remoteOperations.shouldHaveSize(5) // table exposes 2 remote operations

      querySchema.tableOperations.shouldHaveSize(2)
      querySchema.streamOperations.shouldHaveSize(2)

      querySchema.services.map { it.name.fullyQualifiedName }
         .shouldNotContain("FilmsService")
   }

   @Test
   fun `includes everything except those excluded when query declares 'excluded' service operations`() {
      val (_,_,querySchema) = schema.parseQuery("""find { Film[] } excluding { FilmsService::listAllFilms }""")
      querySchema.services.shouldHaveSize(3)
      querySchema.operations.shouldHaveSize(2)
      querySchema.remoteOperations.shouldHaveSize(7) // table exposes 2 remote operations

      querySchema.tableOperations.shouldHaveSize(3)
      querySchema.streamOperations.shouldHaveSize(3)

      querySchema.services.map { it.name.fullyQualifiedName }
         .shouldContain("FilmsService")

      querySchema.service("FilmsService")
         .operations.map { it.name }
         .shouldNotContain("listAllFilms")
   }

}
