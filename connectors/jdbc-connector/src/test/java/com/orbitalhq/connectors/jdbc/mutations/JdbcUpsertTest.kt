package com.orbitalhq.connectors.jdbc.mutations

import com.winterbe.expekt.should
import com.zaxxer.hikari.HikariConfig
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import com.orbitalhq.StubService
import com.orbitalhq.connectors.config.jdbc.JdbcDriver
import com.orbitalhq.connectors.jdbc.*
import com.orbitalhq.connectors.jdbc.query.JdbcQueryTestConfig
import com.orbitalhq.connectors.jdbc.query.MovieRepository
import com.orbitalhq.connectors.jdbc.registry.InMemoryJdbcConnectionRegistry
import com.orbitalhq.query.VyneQlGrammar
import com.orbitalhq.rawObjects
import com.orbitalhq.schema.api.SimpleSchemaProvider
import com.orbitalhq.testVyne
import com.orbitalhq.typedObjects
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.junit4.SpringRunner

@SpringBootTest(classes = [JdbcQueryTestConfig::class])
@RunWith(SpringRunner::class)
class JdbcUpsertTest {
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
   fun `can use a TaxiQL statement to insert a row`(): Unit = runBlocking {
      val vyne = testVyne(
         listOf(
            JdbcConnectorTaxi.schema,
            VyneQlGrammar.QUERY_TYPE_TAXI,
            """
         ${JdbcConnectorTaxi.Annotations.imports}
         import ${VyneQlGrammar.QUERY_TYPE_NAME}
         type MovieId inherits Int
         type MovieTitle inherits String

         // Use a different name from the spring repository, so that we
         // can test DDL creation
         @Table(connection = "movies", schema = "public", table = "film")
         model Film {
            @Id @GeneratedId
            ID : MovieId?
            TITLE : MovieTitle
         }

         @DatabaseService( connection = "movies" )
         service MovieDb {
            table movie : Film[]

            @UpsertOperation
            write operation upsertMovie(Film):Film
         }
      """
         )
      ) { schema -> listOf(JdbcInvoker(connectionFactory, SimpleSchemaProvider(schema))) }
      val result = vyne.query("""
         given { movie : Film = { ID : null , TITLE : "A New Hope" } }
         call MovieDb::upsertMovie
         """.trimIndent())
         .typedObjects()
      result.should.have.size(1)
      result.single()["ID"].shouldNotBeNull()
//      result.first().toRawObject()
//         .should.equal(mapOf("TITLE" to "A New Hope", "ID" to 1))
   }

   @Test
   fun `can insert a batch of rows`():Unit = runBlocking {
      val vyne = testVyne(
         listOf(
            JdbcConnectorTaxi.schema,
            VyneQlGrammar.QUERY_TYPE_TAXI,
            """
         ${JdbcConnectorTaxi.Annotations.imports}
         import ${VyneQlGrammar.QUERY_TYPE_NAME}
         type MovieId inherits Int
         type MovieTitle inherits String

         // Use a different name from the spring repository, so that we
         // can test DDL creation
         @Table(connection = "movies", schema = "public", table = "film")
         parameter model Film {
            @Id @GeneratedId
            ID : MovieId?
            TITLE : MovieTitle
         }

         model Movie {
            title: MovieTitle
         }

         service MovieService {
            operation findAll():Movie[]
         }

         @DatabaseService( connection = "movies" )
         service FilmDb {
            @UpsertOperation
            write operation upsertFilm(Film):Film

            @UpsertOperation
            write operation upsertFilms(Film[]):Film[]
         }
      """
         )
      ) { schema -> listOf(
         JdbcInvoker(connectionFactory, SimpleSchemaProvider(schema)),
         ) }
      val result = vyne.query("""
         given { movies:Movie[] = [ { title : "Star Wars" } , { title : "Back to the Future" } ] }
         call FilmDb::upsertFilms
         """.trimIndent())
         .rawObjects()
      // Worried this could be flakey - will the IDs alway be the same?
      result.forEach { it["ID"].shouldNotBeNull() }

      // Can't work out how to assert this consistently as the IDs change.
//      result.shouldBe(
//         listOf(
//            mapOf("ID" to 1, "TITLE" to "Star Wars"),
//            mapOf("ID" to 2, "TITLE" to "Back to the Future"),
//         )
//      )
   }

}
