package io.vyne.connectors.jdbc.query

import com.winterbe.expekt.should
import com.zaxxer.hikari.HikariConfig
import io.vyne.StubService
import io.vyne.connectors.jdbc.*
import io.vyne.connectors.jdbc.registry.InMemoryJdbcConnectionRegistry
import io.vyne.models.TypedInstance
import io.vyne.query.VyneQlGrammar
import io.vyne.schema.api.SimpleSchemaProvider
import io.vyne.testVyne
import io.vyne.typedObjects
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.junit4.SpringRunner
import java.time.LocalDate
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id

@SpringBootTest(classes = [JdbcQueryTestConfig::class])
@RunWith(SpringRunner::class)
class JdbcQueryTest {
   @Autowired
   lateinit var movieRepository: MovieRepository

   @Autowired
   lateinit var jdbcTemplate: JdbcTemplate

   lateinit var connectionRegistry: InMemoryJdbcConnectionRegistry
   lateinit var connectionFactory: JdbcConnectionFactory

   @Before
   fun setup() {
      val namedParamTemplate = NamedParameterJdbcTemplate(jdbcTemplate)
      connectionRegistry =
         InMemoryJdbcConnectionRegistry(listOf(NamedTemplateConnection("movies", namedParamTemplate, JdbcDriver.H2)))
      connectionFactory = HikariJdbcConnectionFactory(connectionRegistry, HikariConfig())
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
            ID : MovieId
            TITLE : MovieTitle
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
      ) { schema -> listOf(JdbcInvoker(connectionFactory, SimpleSchemaProvider(schema))) }
      val result = vyne.query("""find { Movie[]( MovieTitle == "A New Hope" ) } """)
         .typedObjects()
      result.should.have.size(1)
      result.first().toRawObject()
         .should.equal(mapOf("TITLE" to "A New Hope", "ID" to 1))
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
             ID : MovieId
            TITLE : MovieTitle
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
         """find { Movie[]( MovieTitle == "A New Hope" ) }
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
              ID : MovieId
            TITLE : MovieTitle
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
         """find { NewRelease[] }
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
            ID : MovieId by column("movie id")
            TITLE : MovieTitle by column("movie title")
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
         """find { NewRelease[] }
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
class JdbcQueryTestConfig

interface MovieRepository : JpaRepository<Movie, Int>

interface ActorRepository : JpaRepository<Actor, Int>

@Entity
data class Actor(
   @Id val id: Int,
   val firstName: String,
   val lastName: String
)

