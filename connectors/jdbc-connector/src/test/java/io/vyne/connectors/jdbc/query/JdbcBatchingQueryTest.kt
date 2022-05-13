package io.vyne.connectors.jdbc.query

import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.atLeastOnce
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.winterbe.expekt.should
import io.vyne.StubService
import io.vyne.VersionedSource
import io.vyne.connectors.jdbc.JdbcConnectionFactory
import io.vyne.connectors.jdbc.JdbcConnectorTaxi
import io.vyne.connectors.jdbc.JdbcInvoker
import io.vyne.connectors.jdbc.JdbcOperationBatchingStrategy
import io.vyne.models.TypedInstance
import io.vyne.query.VyneQlGrammar
import io.vyne.schema.api.SimpleSchemaProvider
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.testVyne
import io.vyne.typedObjects
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.mockito.verification.VerificationMode
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.time.LocalDate
import java.util.regex.Pattern

class JdbcBatchingQueryTest {

   val namedParameterJdbcTemplate: NamedParameterJdbcTemplate = mock {}
   val connectionFactory:JdbcConnectionFactory = mock {
      on { jdbcTemplate("movies")} doReturn namedParameterJdbcTemplate
   }

   private val schema = TaxiSchema.fromStrings(listOf(
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
   ))

   @Test
   fun `when projecting a collection, enrichment operations against a db are batched`(): Unit = runBlocking {
      val batchSize = 100
      val stub = StubService(schema = schema)
      val movies = (0 until batchSize).map { i ->
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

      val jdbcInvoker = JdbcInvoker(connectionFactory, SimpleSchemaProvider(schema))

      val vyne = testVyne(
      schema,
      listOf(jdbcInvoker, stub),
      listOf(JdbcOperationBatchingStrategy(jdbcInvoker = jdbcInvoker))
   )
      val result = vyne.query(
         """findAll { NewRelease[] }
         | as {
         |  title : MovieTitle
         |  releaseDate : ReleaseDate
         |}[]
      """.trimMargin()
      )
         .typedObjects()

      val sql = argumentCaptor<String>()
      val paramMap = argumentCaptor<Map<String, Any>>()
      verify(namedParameterJdbcTemplate, atLeastOnce()).queryForList(sql.capture(), paramMap.capture())
      val batchSql = sql.firstValue
      val queryIdPattern =  Pattern.compile(".*'([^']*)'.*")
      val queryIdMatch = queryIdPattern.matcher(batchSql)
      queryIdMatch.find().should.be.`true`
      val lastQueryId = queryIdMatch.group(1)
      val batchCount = lastQueryId.last().digitToInt()
      batchCount.should.be.above(1)
   }
}
