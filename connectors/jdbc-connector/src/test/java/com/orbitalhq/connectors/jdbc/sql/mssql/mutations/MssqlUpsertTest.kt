package com.orbitalhq.connectors.jdbc.sql.mssql.mutations

import com.orbitalhq.connectors.config.jdbc.JdbcUrlAndCredentials
import com.orbitalhq.connectors.config.jdbc.JdbcUrlCredentialsConnectionConfiguration
import com.orbitalhq.connectors.jdbc.HikariJdbcConnectionFactory
import com.orbitalhq.connectors.jdbc.JdbcConnectionFactory
import com.orbitalhq.connectors.jdbc.JdbcConnectorTaxi
import com.orbitalhq.connectors.jdbc.JdbcInvoker
import com.orbitalhq.connectors.jdbc.registry.InMemoryJdbcConnectionRegistry
import com.orbitalhq.connectors.jdbc.sql.mssql.query.MovieRepository
import com.orbitalhq.connectors.jdbc.sql.mssql.query.MsSQLQueryTestConfig
import com.orbitalhq.query.VyneQlGrammar
import com.orbitalhq.rawObjects
import com.orbitalhq.schema.api.SimpleSchemaProvider
import com.orbitalhq.testVyne
import com.orbitalhq.typedObjects
import com.winterbe.expekt.should
import com.zaxxer.hikari.HikariConfig
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.containers.MSSQLServerContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@SpringBootTest(classes = [MsSQLQueryTestConfig::class],
   properties = ["spring.jpa.hibernate.ddl-auto=create"]
)
class MssqlUpsertTest {
   @Autowired
   lateinit var movieRepository: MovieRepository

   lateinit var connectionRegistry: InMemoryJdbcConnectionRegistry
   lateinit var connectionFactory: JdbcConnectionFactory



   companion object {
      @JvmField
      @Container
      @ServiceConnection
      val mssqlContainer = MSSQLServerContainer("mcr.microsoft.com/mssql/server")
         .acceptLicense()
   }

   @BeforeEach
   fun setup() {
      mssqlContainer.waitingFor(Wait.forListeningPort())
      val connectionDetails = JdbcUrlCredentialsConnectionConfiguration(
         "movies",
         "MSSQL",
         JdbcUrlAndCredentials(
            mssqlContainer.getJdbcUrl(),
            mssqlContainer.username,
            mssqlContainer.password
         )
      )
      connectionRegistry =
         InMemoryJdbcConnectionRegistry(listOf(connectionDetails))
      connectionFactory = HikariJdbcConnectionFactory(connectionRegistry, HikariConfig())
   }

   fun vyne() =  testVyne(
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

            @InsertOperation
            write operation insertMovie(Film):Film

            @UpdateOperation
            write operation updateMovie(Film):Film

            @InsertOperation
            write operation insertFilms(Film[]):Film[]
         }
      """
      )
   ) { schema -> listOf(JdbcInvoker(connectionFactory, SimpleSchemaProvider(schema), SimpleMeterRegistry())) }
   @Test
   fun `can use a TaxiQL statement to insert a row`(): Unit = runBlocking {
      val result = vyne().query("""
         given { movie : Film = { ID : null , TITLE : "A New Hope" } }
         call MovieDb::insertMovie
         """.trimIndent())
         .typedObjects()
      result.should.have.size(1)
      result.single()["ID"].shouldNotBeNull()
   }

   @Test
   fun `can use a TaxiQL statement to update a row`(): Unit = runBlocking {
      val vyne = vyne()
      val insert = vyne.query("""
         given { movie : Film = { ID : null , TITLE : "A New Hope" } }
         call MovieDb::insertMovie
         """.trimIndent())
         .typedObjects()
      insert.should.have.size(1)
      val id = insert.single()["ID"].shouldNotBeNull().value as Int

      val update = vyne.query("""
         given { movie : Film = { ID : $id , TITLE : "Star Wars IV - A New Hope" } }
         call MovieDb::updateMovie
         """.trimIndent())
         .typedObjects()
      update.should.have.size(1)
      update.single().toRawObject()
         .shouldBe(mapOf(
            "ID" to id,
            "TITLE" to "Star Wars IV - A New Hope"
         ))
   }

   @Test
   fun `can insert a batch of rows`():Unit = runBlocking {
      val vyne = vyne()

      val result = vyne.query("""
         given { movies:Film[] = [ { TITLE : "Star Wars" } , { TITLE : "Back to the Future" } ] }
         call MovieDb::insertFilms
         """.trimIndent())
         .rawObjects()
      result.forEach { it["ID"].shouldNotBeNull() }
   }

}
