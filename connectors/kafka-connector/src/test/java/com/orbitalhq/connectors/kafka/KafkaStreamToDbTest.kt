package com.orbitalhq.connectors.kafka

import com.jayway.awaitility.Awaitility
import com.nhaarman.mockito_kotlin.mock
import com.orbitalhq.connectors.config.jdbc.JdbcDriver
import com.orbitalhq.connectors.config.jdbc.JdbcUrlAndCredentials
import com.orbitalhq.connectors.config.jdbc.JdbcUrlCredentialsConnectionConfiguration
import com.orbitalhq.connectors.jdbc.HikariJdbcConnectionFactory
import com.orbitalhq.connectors.jdbc.JdbcConnectionFactory
import com.orbitalhq.connectors.jdbc.JdbcConnectorTaxi
import com.orbitalhq.connectors.jdbc.JdbcInvoker
import com.orbitalhq.connectors.jdbc.NamedTemplateConnection
import com.orbitalhq.connectors.jdbc.SimpleJdbcConnectionFactory
import com.orbitalhq.connectors.jdbc.registry.InMemoryJdbcConnectionRegistry
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.schema.api.SimpleSchemaProvider
import com.orbitalhq.testVyne
import com.zaxxer.hikari.HikariConfig
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import kotlinx.coroutines.runBlocking
import org.jooq.impl.DSL
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.test.context.junit4.SpringRunner
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.util.concurrent.TimeUnit

class KafkaStreamToDbTest : BaseKafkaContainerTest() {

   lateinit var jdbcUrl: String
   lateinit var username: String
   lateinit var password: String


   lateinit var jdbcConnectionRegistry: InMemoryJdbcConnectionRegistry
   lateinit var jdbcConnectionFactory: JdbcConnectionFactory

   @Rule
   @JvmField
   final val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:11.1") as PostgreSQLContainer<*>

   @Before
   override fun before() {
      super.before()
      postgreSQLContainer.start()
      postgreSQLContainer.waitingFor(Wait.forListeningPort())

      jdbcUrl = postgreSQLContainer.jdbcUrl
      username = postgreSQLContainer.username
      password = postgreSQLContainer.password

      jdbcConnectionRegistry = InMemoryJdbcConnectionRegistry()
      jdbcConnectionRegistry.register(
         JdbcUrlCredentialsConnectionConfiguration(
            "postgres",
            JdbcDriver.POSTGRES,
            JdbcUrlAndCredentials(jdbcUrl, username, password)
         )
      )
//      jdbcConnectionFactory = SimpleJdbcConnectionFactory()
      //      jdbcConnectionRegistry =
//         InMemoryJdbcConnectionRegistry(listOf(NamedTemplateConnection("movies", namedParamTemplate, JdbcDriver.H2)))
      jdbcConnectionFactory = HikariJdbcConnectionFactory(jdbcConnectionRegistry, HikariConfig())

   }


   @Test
   fun `can stream from kafka to db inserts`() {
      buildProducer(connectionName = "moviesKafka")


      val schema = """

         ${JdbcConnectorTaxi.Annotations.imports}
         ${KafkaConnectorTaxi.Annotations.imports}

         type MovieId inherits String
         type MovieTitle inherits String

         model MovieEvent {
            id : MovieId
            title : MovieTitle
         }

         @KafkaService( connectionName = "moviesKafka" )
         service MovieService {
            @KafkaOperation( topic = "movies", offset = "earliest" )
            stream streamMovieQuery:Stream<MovieEvent>
         }

         // Use a different name from the spring repository, so that we
         // can test DDL creation
         @Table(connection = "postgres", schema = "public", table = "film")
         parameter model FilmDbRecord {
            @Id @GeneratedId
            DB_ID : FilmDbId inherits Int
            FILM_ID : MovieId
            TITLE : MovieTitle
         }

         @DatabaseService( connection = "postgres" )
         service MovieDb {
            @UpsertOperation
            write operation upsertMovie(FilmDbRecord):FilmDbRecord
         }
      """.trimIndent()


      val vyne = testVyne(
         listOf(
            JdbcConnectorTaxi.schema,
            KafkaConnectorTaxi.schema,
            schema
         )
      ) { schema ->
         val kafkaStreamManager =
            KafkaStreamManager(connectionRegistry, SimpleSchemaProvider(schema), formatRegistry = formatRegistry)
         listOf(
            JdbcInvoker(jdbcConnectionFactory, SimpleSchemaProvider(schema)),
            KafkaInvoker(kafkaStreamManager, mock { })
         )
      }

      val resultsFromQuery = mutableListOf<TypedInstance>()
      val query = runBlocking {
         vyne.query(
            """stream { MovieEvent } as FilmDbRecord[]
            |call MovieDb::upsertMovie
         """.trimMargin()
         )
      }
      collectQueryResults(query, resultsFromQuery)

      sendMessage("""{ "id" : "one" , "title" : "Star Wars" }""")
      sendMessage("""{ "id" : "two" , "title" : "Empire Strikes Back" }""")

      // Now test an upsert
      sendMessage("""{ "id" : "one" , "title" : "Star Wars IV: A New Hope" }""")

      Awaitility.await().atMost(10, TimeUnit.MINUTES).until<Boolean> { resultsFromQuery.size == 3 }

      val sqlDsl = jdbcConnectionFactory.dsl(jdbcConnectionRegistry.getConnection("postgres"))
      val dbQueryResults = sqlDsl.select(DSL.asterisk()).from(DSL.name("film"))
         .fetchMaps()

      dbQueryResults.shouldHaveSize(3)

      dbQueryResults.map { listOf("title" to it["TITLE"], "id" to it["FILM_ID"]).toMap() }
         .shouldContainAll(
            mapOf("id" to "one", "title" to "Star Wars"),
            mapOf("id" to "one", "title" to "Star Wars IV: A New Hope"),
            mapOf("id" to "two", "title" to "Empire Strikes Back"),
         )
   }


   @Test
   fun `can stream from kafka to db upserts`() {
      buildProducer(connectionName = "moviesKafka")


      val schema = """

         ${JdbcConnectorTaxi.Annotations.imports}
         ${KafkaConnectorTaxi.Annotations.imports}

         type MovieId inherits String
         type MovieTitle inherits String

         model MovieEvent {
            id : MovieId
            title : MovieTitle
         }

         @KafkaService( connectionName = "moviesKafka" )
         service MovieService {
            @KafkaOperation( topic = "movies", offset = "earliest" )
            stream streamMovieQuery:Stream<MovieEvent>
         }

         // Use a different name from the spring repository, so that we
         // can test DDL creation
         @Table(connection = "postgres", schema = "public", table = "film")
         parameter model FilmDbRecord {
            @Id
            FILM_ID : MovieId
            TITLE : MovieTitle
         }

         @DatabaseService( connection = "postgres" )
         service MovieDb {
            @UpsertOperation
            write operation upsertMovie(FilmDbRecord):FilmDbRecord
         }
      """.trimIndent()


      val vyne = testVyne(
         listOf(
            JdbcConnectorTaxi.schema,
            KafkaConnectorTaxi.schema,
            schema
         )
      ) { schema ->
         val kafkaStreamManager =
            KafkaStreamManager(connectionRegistry, SimpleSchemaProvider(schema), formatRegistry = formatRegistry)
         listOf(
            JdbcInvoker(jdbcConnectionFactory, SimpleSchemaProvider(schema)),
            KafkaInvoker(kafkaStreamManager, mock {})
         )
      }

      val resultsFromQuery = mutableListOf<TypedInstance>()
      val query = runBlocking {
         vyne.query(
            """stream { MovieEvent } as FilmDbRecord[]
            |call MovieDb::upsertMovie
         """.trimMargin()
         )
      }
      collectQueryResults(query, resultsFromQuery)

      sendMessage("""{ "id" : "one" , "title" : "Star Wars" }""")
      sendMessage("""{ "id" : "two" , "title" : "Empire Strikes Back" }""")

      // Make sure both messages are processed before sending the upsert
      Awaitility.await().atMost(10, TimeUnit.SECONDS).until<Boolean> { resultsFromQuery.size == 2 }

      // Now test an upsert
      sendMessage("""{ "id" : "one" , "title" : "Star Wars IV: A New Hope" }""")

      Awaitility.await().atMost(10, TimeUnit.SECONDS).until<Boolean> { resultsFromQuery.size == 3 }

      val sqlDsl = jdbcConnectionFactory.dsl(jdbcConnectionRegistry.getConnection("postgres"))
      val dbQueryResults = sqlDsl.select(DSL.asterisk()).from(DSL.name("film"))
         .fetchMaps()

      dbQueryResults.shouldHaveSize(2)

      dbQueryResults.map { listOf("title" to it["TITLE"], "id" to it["FILM_ID"]).toMap() }
         .shouldContainAll(
            mapOf("id" to "one", "title" to "Star Wars IV: A New Hope"),
            mapOf("id" to "two", "title" to "Empire Strikes Back"),
         )
   }


}

