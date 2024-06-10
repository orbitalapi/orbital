package com.orbitalhq.cockpit.core.query

import com.orbitalhq.connectors.jdbc.JdbcConnectorTaxi
import com.orbitalhq.connectors.kafka.KafkaConnectorTaxi
import com.orbitalhq.query.VyneQlGrammar
import com.orbitalhq.schemas.taxi.TaxiSchema
import io.kotest.common.runBlocking
import io.kotest.matchers.collections.shouldHaveSize
import lang.taxi.annotations.HttpOperation
import org.junit.jupiter.api.Test

class QueryVisualizerServiceTest {
   @Test
   fun `can get a query plan using stub services and stub values`() :Unit = runBlocking{
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
      val service = QueryVisualizerService()
      val query = """find { foo.Film[] } as {
         id : foo.FilmId
         score : foo.ReviewScore
      }[]
      """
      val diagramRows = service.visualizeQuery(
         query.trimMargin(), schema
      )
      diagramRows.shouldHaveSize(3)
   }


   @Test
   fun `can get a query plan using stub services and stub values against a stream`() :Unit = runBlocking{
      val schema = TaxiSchema.from(
         """
         ${VyneQlGrammar.QUERY_TYPE_TAXI}
          ${KafkaConnectorTaxi.schema}
         namespace foo {
            model Film {
               id : FilmId inherits Int
            }
            model Review {
               score : ReviewScore inherits Int
            }
           @KafkaService( connectionName = "moviesConnection" )
            service FilmsTopic {
            @KafkaOperation( topic = "someTopic", offset = "earliest" )
               stream films : Stream<Film>
            }
            service ReviewsApi {
               @${HttpOperation.NAME}(method = "GET", url="http://fakeUrl")
               operation getReview(FilmId):Review
            }
         }
      """.trimIndent()
      )
      val service = QueryVisualizerService()
      val query = """stream { foo.Film } as {
         id : foo.FilmId
         score : foo.ReviewScore
      }[]
      """
      val diagramRows = service.visualizeQuery(
         query.trimMargin(), schema
      )
      diagramRows.shouldHaveSize(3)
   }
}
