package io.vyne.connectors.jdbc.query

import com.nhaarman.mockito_kotlin.mock
import com.winterbe.expekt.should
import io.vyne.StubService
import io.vyne.connectors.jdbc.JdbcConnectionFactory
import io.vyne.connectors.jdbc.JdbcConnectorTaxi
import io.vyne.connectors.jdbc.JdbcInvoker
import io.vyne.models.TypedInstance
import io.vyne.query.VyneQlGrammar
import io.vyne.schema.api.SimpleSchemaProvider
import io.vyne.testVyne
import io.vyne.typedObjects
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.time.LocalDate

class JdbcBatchingQueryTest {

   val connectionFactory:JdbcConnectionFactory = mock {  }

   @Test
   fun `when projecting a collection, enrichment operations against a db are batched`(): Unit = runBlocking {

      val vyne = testVyne(
         listOf(
            JdbcConnectorTaxi.schema,
            VyneQlGrammar.QUERY_TYPE_TAXI,
            """
         ${JdbcConnectorTaxi.Annotations.imports}
         import ${VyneQlGrammar.QUERY_TYPE_NAME}
         type MovieId inherits Int
         type MovieTitle inherits String
         type AvailableCopyCount inherits Int
         @Table(connection = "movies", table = "movie", schema = "public")
         model Movie {
            @Id
            id : MovieId
            title : MovieTitle
         }

         model NewRelease {
            movieId : MovieId
            releaseDate : ReleaseDate inherits Date
         }

         service ApiService {
            operation getNewReleases():NewRelease[]
         }

         @DatabaseService( connection = "movies" )
         service MovieDb {
            vyneQl query findOneMovie(body:VyneQlQuery):Movie with capabilities {
                  filter(==,in,like),
                  sum,
                  count
               }

         }
      """
         )
      ) { schema ->

         val stub = StubService(schema = schema)
         val movies = (1 until 100).map { i ->
            TypedInstance.from(
               schema.type("NewRelease"), mapOf(
                  "movieId" to i,
                  // A new Fast and Furious movie every christmas for 100 years.
                  "releaseDate" to LocalDate.parse("${(2000+i)}-12-25")
               ), schema
            )
         }
         stub.addResponse(
            "getNewReleases",
            movies,
            modifyDataSource = true
         )
         listOf(JdbcInvoker(connectionFactory, SimpleSchemaProvider(schema)), stub)
      }
      val result = vyne.query(
         """findAll { NewRelease[] }
         | as {
         |  title : MovieTitle
         |  releaseDate : ReleaseDate
         |}[]
      """.trimMargin()
      )
         .typedObjects()

      result.should.have.size(100)


      result.first().toRawObject()
         .should.equal(mapOf("title" to "A New Hope", "releaseDate" to "1979-05-10"))
   }


}
