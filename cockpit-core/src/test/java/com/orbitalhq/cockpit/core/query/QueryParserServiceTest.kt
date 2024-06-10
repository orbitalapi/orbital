package com.orbitalhq.cockpit.core.query

import com.orbitalhq.connectors.jdbc.JdbcConnectorTaxi
import com.orbitalhq.query.VyneQlGrammar
import com.orbitalhq.schema.consumer.SimpleSchemaStore
import com.orbitalhq.schemas.taxi.TaxiSchema
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import lang.taxi.annotations.HttpOperation
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
      parsed.hasCompilationErrors.shouldBeFalse()
   }

   @Test
   fun `when theres no way to execute a query a message is returned`() {
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
      parsed.hasQueryErrors.shouldBeTrue()
      parsed.queryPlan.queryExecutionMessages.single()
         .message.shouldBe("No data sources were found that can return Film[]")
   }

   @Test
   fun `when query execution is possible then returns a query plan`() {
      val schema = TaxiSchema.from(
         """
         ${VyneQlGrammar.QUERY_TYPE_TAXI}
         ${JdbcConnectorTaxi.schema}
         namespace foo {
            model Film {
               id : FilmId inherits Int
            }
            model Review {
               score : ReviewScore inherits Int
            }
            @com.orbitalhq.jdbc.DatabaseService(connection = "myConnection")
            service FilmsDb {
               table film : Film[]
            }
            service ReviewsApi {
               @${HttpOperation.NAME}(method = "GET", url="http://fakeUrl")
               operation getReview(FilmId):Review
            }
         }
      """.trimIndent()
      )
      val query = """find { foo.Film[] } as {
         id : foo.FilmId
         score : foo.ReviewScore
      }[]
      """

      val schemaStore = SimpleSchemaStore.forSchema(schema)
      val service = QueryParserService(schemaStore)

      val parsed = service.parseQuery(query)
         .block()!!

      parsed.queryPlan.steps.shouldHaveSize(3)
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
      parsed.hasCompilationErrors.shouldBeTrue()
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
      parsed.hasCompilationErrors.shouldBeTrue()
   }
}
