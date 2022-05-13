package io.vyne.connectors.jdbc.query

import com.winterbe.expekt.should
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.vyne.StubService
import io.vyne.connectors.jdbc.BatchTraceCollector
import io.vyne.connectors.jdbc.HikariJdbcConnectionFactory
import io.vyne.connectors.jdbc.JdbcConnectionFactory
import io.vyne.connectors.jdbc.JdbcConnectorTaxi
import io.vyne.connectors.jdbc.JdbcDriver
import io.vyne.connectors.jdbc.JdbcInvoker
import io.vyne.connectors.jdbc.JdbcOperationBatchingStrategy
import io.vyne.connectors.jdbc.NamedTemplateConnection
import io.vyne.connectors.jdbc.registry.InMemoryJdbcConnectionRegistry
import io.vyne.connectors.jdbc.registry.JdbcConnectionRegistry
import io.vyne.models.TypedInstance
import io.vyne.query.VyneQlGrammar
import io.vyne.schema.api.SimpleSchemaProvider
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.testVyne
import io.vyne.typedObjects
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.junit4.SpringRunner
import java.time.LocalDate
import java.util.regex.Pattern
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id

@SpringBootTest(classes = [JdbcQueryTestConfig::class])
@RunWith(SpringRunner::class)
class JdbcQueryTest {
   @Autowired
   lateinit var movieRepository: MovieRepository

   @Autowired
   lateinit var connectionFactory: JdbcConnectionFactory

   @Test
   fun canStart() {
      val movie = Movie("1", "A New Hope")
      movieRepository.save(
         movie
      )

      movieRepository.count().should.equal(1)
   }

   @Test
   fun `can use a TaxiQL statement to query a db`(): Unit = runBlocking {
      movieRepository.save(
         Movie("1", "A New Hope")
      )
      val vyne = testVyne(
         listOf(
            JdbcConnectorTaxi.schema,
            VyneQlGrammar.QUERY_TYPE_TAXI,
            """
         ${JdbcConnectorTaxi.Annotations.imports}
         import ${VyneQlGrammar.QUERY_TYPE_NAME}
         type MovieId inherits Int
         type MovieTitle inherits String

         @Table(connection = "movies", schema = "public", table = "movie")
         model Movie {
            id : MovieId
            title : MovieTitle
         }

         @DatabaseService( connection = "movies" )
         service MovieDb {
            vyneQl query movieQuery(body:VyneQlQuery):Movie[] with capabilities {
                  filter(==,in,like),
                  sum,
                  count
               }
         }
      """
         )) { schema -> listOf(JdbcInvoker(connectionFactory, SimpleSchemaProvider(schema))) }
      val result = vyne.query("""findAll { Movie[]( MovieTitle == "A New Hope" ) } """)
         .typedObjects()
      result.should.have.size(1)
      result.first().toRawObject()
         .should.equal(mapOf("title" to "A New Hope", "id" to 1))
   }

   @Test
   fun `can issue a query that starts with jdbc and joins to api`(): Unit = runBlocking {
      movieRepository.save(
         Movie("1", "A New Hope")
      )
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
            id : MovieId
            title : MovieTitle
         }

         service ApiService {
            operation getAvailableCopies(MovieId):AvailableCopyCount
         }

         @DatabaseService( connection = "movies" )
         service MovieDb {
            vyneQl query movieQuery(body:VyneQlQuery):Movie[] with capabilities {
                  filter(==,in,like),
                  sum,
                  count
               }
         }
      """
         )
      ) { schema ->

         val stub = StubService(schema = schema)
         stub.addResponse(
            "getAvailableCopies",
            TypedInstance.from(schema.type("AvailableCopyCount"), 150, schema),
            modifyDataSource = true
         )
         listOf(JdbcInvoker(connectionFactory, SimpleSchemaProvider(schema)), stub)
      }
      val result = vyne.query(
         """findAll { Movie[]( MovieTitle == "A New Hope" ) }
         | as {
         |  title : MovieTitle
         |  availableCopies : AvailableCopyCount
         |}[]
      """.trimMargin()
      )
         .typedObjects()
      result.should.have.size(1)
      result.first().toRawObject()
         .should.equal(mapOf("title" to "A New Hope", "availableCopies" to 150))
   }

   @Test
   fun `can issue a query that starts with api and joins to jdbc`(): Unit = runBlocking {
      movieRepository.save(
         Movie("1", "A New Hope")
      )
      val schema = TaxiSchema.fromStrings(listOf(
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

      val jdbcInvoker = JdbcInvoker(connectionFactory, SimpleSchemaProvider(schema))
      val stub = StubService(schema = schema)
      stub.addResponse(
         "getNewReleases",
         TypedInstance.from(
            schema.type("NewRelease"), mapOf(
            "movieId" to 1,
            "releaseDate" to LocalDate.parse("1979-05-10")
         ), schema
         ),
         modifyDataSource = true
      )
      val invokers = listOf(jdbcInvoker, stub)
      val vyne = testVyne(
         schema,
         invokers,
         listOf(JdbcOperationBatchingStrategy(jdbcInvoker))
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
      result.should.have.size(1)
      result.first().toRawObject()
         .should.equal(mapOf("title" to "A New Hope", "releaseDate" to "1979-05-10"))
   }

   @Test
   fun `can issue a query that starts with api and joins to jdbc for csv model`(): Unit = runBlocking {
      movieRepository.save(
         Movie("1", "A New Hope")
      )
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
            id : MovieId by column("movie id")
            title : MovieTitle by column("movie title")
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
         stub.addResponse(
            "getNewReleases",
            TypedInstance.from(
               schema.type("NewRelease"), mapOf(
               "movieId" to 1,
               "releaseDate" to LocalDate.parse("1979-05-10")
            ), schema
            ),
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
      result.should.have.size(1)
      result.first().toRawObject()
         .should.equal(mapOf("title" to "A New Hope", "releaseDate" to "1979-05-10"))
   }

   @Test
   fun `can issue a query that starts with api and joins to jdbc by using batching`(): Unit = runBlocking {
      movieRepository.saveAll(
         listOf(
            Movie("1", "A New Hope"),
            Movie("2", "There is No Hope")
         )
      )
      val schema = TaxiSchema.fromStrings(listOf(
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

      val jdbcInvoker = JdbcInvoker(connectionFactory, SimpleSchemaProvider(schema))
      val stub = StubService(schema = schema)
      stub.addResponse(
         "getNewReleases",
         listOf(
            TypedInstance.from(
               schema.type("NewRelease"), mapOf(
               "movieId" to 1,
               "releaseDate" to LocalDate.parse("1979-05-10")
            ), schema
            ),
            TypedInstance.from(
               schema.type("NewRelease"), mapOf(
               "movieId" to 2,
               "releaseDate" to LocalDate.parse("1979-06-10")
            ), schema
            )
         ),
         modifyDataSource = true
      )
      val invokers = listOf(jdbcInvoker, stub)
      data class TestBatchTraceCollection(var sqlQuery: String = "", var parameterNameValueMap: Map< String, Any> = emptyMap()): BatchTraceCollector {
         override fun reportSqlBatchQuery(sqlQuery: String, parameterNameValueMap: Map<String, Any>) {
            this.sqlQuery = sqlQuery
            this.parameterNameValueMap = parameterNameValueMap
         }

      }
      val batchTraceCollector = TestBatchTraceCollection()
      val vyne = testVyne(
         schema,
         invokers,
         listOf(JdbcOperationBatchingStrategy(jdbcInvoker, batchTraceCollector))
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
      result.should.have.size(2)
      val queryIdPattern =  Pattern.compile(".*'([^']*)'.*")
      val queryIdMatch = queryIdPattern.matcher(batchTraceCollector.sqlQuery)
      queryIdMatch.find().should.be.`true`
      val secondQueryId  = queryIdMatch.group(1)
      val firstQueryId = "${secondQueryId.dropLast(1)}0"
      batchTraceCollector.sqlQuery.should.equal("select '$firstQueryId' as _queryId, * from movie t0 WHERE t0.id = :id00 UNION ALL select '$secondQueryId' as _queryId, * from movie t0 WHERE t0.id = :id01")
      result.first().toRawObject()
         .should.equal(mapOf("title" to "A New Hope", "releaseDate" to "1979-05-10"))


   }


}

@Entity
data class Movie(
   @Id
   @Column
   val id: String,
   @Column
   val title: String,
)



@Configuration
@EnableAutoConfiguration
@EnableJpaRepositories
class JdbcQueryTestConfig {
   @Bean
   fun namedParameterJdbcTemplate(jdbcTemplate: JdbcTemplate) = NamedParameterJdbcTemplate(jdbcTemplate)

   @Bean
   fun inMemoryJdbcConnectionRegistry(namedParameterJdbcTemplate: NamedParameterJdbcTemplate) =
      InMemoryJdbcConnectionRegistry(listOf(NamedTemplateConnection(
         "movies",
         namedParameterJdbcTemplate,
         JdbcDriver.H2)))

   @Bean
   fun connectionFactory(connectionRegistry: JdbcConnectionRegistry): JdbcConnectionFactory = HikariJdbcConnectionFactory(connectionRegistry, HikariConfig())
}

interface MovieRepository : JpaRepository<Movie, Int>

interface ActorRepository : JpaRepository<Actor, Int>

@Entity
data class Actor(
   @Id val id: Int,
   val firstName: String,
   val lastName: String
)

