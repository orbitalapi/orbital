package io.vyne.connectors.jdbc

import com.winterbe.expekt.should
import io.vyne.query.queryBuilders.VyneQlGrammar
import io.vyne.schemaStore.SimpleSchemaProvider
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
import javax.persistence.Entity
import javax.persistence.Id

@SpringBootTest(classes = [TestConfig::class])
@RunWith(SpringRunner::class)
class JdbcQueryTest {
   @Autowired
   lateinit var movieRepository: MovieRepository

   @Autowired
   lateinit var jdbcTemplate: JdbcTemplate

   lateinit var connectionRegistry: JdbcConnectionRegistry

   @Before
   fun setup() {
      val namedParamTemplate = NamedParameterJdbcTemplate(jdbcTemplate)
      connectionRegistry = JdbcConnectionRegistry(listOf(SimpleJdbcConnectionProvider("movies", namedParamTemplate, JdbcDriver.H2)))
   }

   @Test
   fun canStart() {
      movieRepository.save(
         Movie(1, "A New Hope")
      )

      movieRepository.count().should.equal(1)
   }

   @Test
   fun `can use a TaxiQL statement to query a db`(): Unit = runBlocking {
      movieRepository.save(
         Movie(1, "A New Hope")
      )
      val vyne = testVyne(
         listOf(
            Taxi.schema,
            VyneQlGrammar.QUERY_TYPE_TAXI,
            """
         ${Taxi.Annotations.imports}
         import ${VyneQlGrammar.QUERY_TYPE_NAME}
         type MovieId inherits Int
         type MovieTitle inherits String

         @Table(name = "movie")
         model Movie {
            id : MovieId
            title : MovieTitle
         }

         @DatabaseService( connectionName = "movies" )
         service MovieDb {
            vyneQl query movieQuery(body:VyneQlQuery):Movie[] with capabilities {
                  filter(=,in,like),
                  sum,
                  count
               }
         }
      """
         )
      ) { schema -> listOf(JdbcInvoker(connectionRegistry, SimpleSchemaProvider(schema))) }
      val result = vyne.query("""findAll { Movie[]( MovieTitle = "A New Hope" ) } """)
         .typedObjects()
      result.should.have.size(1)
      result.first().toRawObject()
         .should.equal(mapOf("title" to "A New Hope", "id" to 1))
   }
}


@Entity
data class Movie(
   @Id
   val id: Int,
   val title: String,
)

interface MovieRepository : JpaRepository<Movie, Int>

@Configuration
@EnableAutoConfiguration
@EnableJpaRepositories
class TestConfig

@Entity
data class Actor(
   @Id val id: Int,
   val firstName: String,
   val lastName: String
)

interface ActorRepository : JpaRepository<Actor, Int>
