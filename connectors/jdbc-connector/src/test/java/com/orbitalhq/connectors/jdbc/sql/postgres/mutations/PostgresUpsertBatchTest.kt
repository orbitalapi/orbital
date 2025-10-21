package com.orbitalhq.connectors.jdbc.sql.postgres.mutations

import com.orbitalhq.connectors.config.jdbc.JdbcUrlAndCredentials
import com.orbitalhq.connectors.config.jdbc.JdbcUrlCredentialsConnectionConfiguration
import com.orbitalhq.connectors.jdbc.HikariJdbcConnectionFactory
import com.orbitalhq.connectors.jdbc.JdbcConnectionFactory
import com.orbitalhq.connectors.jdbc.JdbcConnectorTaxi
import com.orbitalhq.connectors.jdbc.JdbcInvoker
import com.orbitalhq.connectors.jdbc.NamedTemplateConnection
import com.orbitalhq.connectors.jdbc.SimpleJdbcConnectionFactory
import com.orbitalhq.connectors.jdbc.drivers.postgres.PostgresDbSupport
import com.orbitalhq.connectors.jdbc.registry.InMemoryJdbcConnectionRegistry
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.VyneQlGrammar
import com.orbitalhq.rawObjects
import com.orbitalhq.schema.api.SimpleSchemaProvider
import com.orbitalhq.testVyneWithStub
import com.zaxxer.hikari.HikariConfig
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import kotlin.random.Random

class PostgresUpsertBatchTest {
   lateinit var jdbcUrl: String
   lateinit var username: String
   lateinit var password: String

   lateinit var connectionRegistry: InMemoryJdbcConnectionRegistry
   lateinit var connectionFactory: JdbcConnectionFactory
   @Rule
   @JvmField
   val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:11.1").apply {
      withPassword("")
      withInitScript("postgres/actor-schema.sql")
   }
      as PostgreSQLContainer<*>


   @BeforeEach
   fun before() {
      postgreSQLContainer.start()
      postgreSQLContainer.waitingFor(Wait.forListeningPort())

      jdbcUrl = postgreSQLContainer.jdbcUrl
      username = postgreSQLContainer.username
      password = postgreSQLContainer.password
      val connectionDetails = JdbcUrlCredentialsConnectionConfiguration(
         "postgres",
         PostgresDbSupport.driverName,
         JdbcUrlAndCredentials(jdbcUrl, username, password)
      )
      val template = SimpleJdbcConnectionFactory()
         .jdbcTemplate(connectionDetails)
      val connectionRegistry =
         InMemoryJdbcConnectionRegistry(listOf(NamedTemplateConnection("movies", template, "POSTGRES")))
      connectionFactory = HikariJdbcConnectionFactory(connectionRegistry, HikariConfig())

   }

   fun vyne() =  testVyneWithStub(
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
         @Table(connection = "movies", schema = "public", table = "film__${Random.nextInt(100_000)}")
         closed parameter model Film {
            @Id @GeneratedId
            ID : MovieId?
            TITLE : MovieTitle
         }

         service TitleApi {
            operation getTitles():MovieTitle[]
         }

         @DatabaseService( connection = "movies" )
         service MovieDb {
            table movie : Film[]

            @InsertOperation(batchSize = 5, batchDuration = 1000)
            write operation insertMovie(Film):Film

            @UpdateOperation(batchSize = 5, batchDuration = 1000)
            write operation updateMovie(Film):Film
         }
      """
      )
   ) { schema -> listOf(JdbcInvoker(connectionFactory, SimpleSchemaProvider(schema), SimpleMeterRegistry())) }

   @Test
   fun `can insert a batch of rows`():Unit = runBlocking {
      val (vyne,stub) = vyne()

      val movies = TypedCollection.from(listOf(
         "Inception",
         "The Shawshank Redemption",
         "Interstellar",
         "The Godfather",
         "The Dark Knight",
         "Pulp Fiction",
         "Forrest Gump",
         "The Matrix",
         "Parasite",
         "Fight Club",
         "The Social Network",
         "Whiplash",
         "Arrival"
      ).map { TypedInstance.from(vyne.type("MovieTitle"), it, vyne.schema) })
      stub.addResponse("getTitles", movies)


      val result = vyne.query("""
         find { MovieTitle[] }
         call MovieDb::insertMovie
         """.trimIndent())
         .rawObjects()
      result.shouldHaveSize(movies.size)
      val readResult = vyne.query("""find { Film[] }""")
         .rawObjects()
      readResult.shouldHaveSize(movies.size)
      readResult.forEach { it["ID"].shouldNotBeNull() }
   }
}
