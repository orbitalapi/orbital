package com.orbitalhq.connectors.jdbc.sql.mysql.mutations

import com.orbitalhq.connectors.config.jdbc.JdbcDriver
import com.orbitalhq.connectors.config.jdbc.JdbcUrlAndCredentials
import com.orbitalhq.connectors.config.jdbc.JdbcUrlCredentialsConnectionConfiguration
import com.orbitalhq.connectors.jdbc.*
import com.orbitalhq.connectors.jdbc.registry.InMemoryJdbcConnectionRegistry
import com.orbitalhq.connectors.jdbc.sql.mysql.query.MovieRepository
import com.orbitalhq.connectors.jdbc.sql.mysql.query.MySqlQueryTestConfig
import com.orbitalhq.query.VyneQlGrammar
import com.orbitalhq.rawObjects
import com.orbitalhq.schema.api.SimpleSchemaProvider
import com.orbitalhq.testVyne
import com.orbitalhq.typedObjects
import com.winterbe.expekt.should
import com.zaxxer.hikari.HikariConfig
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.sql.DriverManager
import java.sql.SQLException

@Testcontainers
@SpringBootTest(
   classes = [MySqlQueryTestConfig::class],
   properties = [
      "spring.jpa.hibernate.ddl-auto=create",
      "spring.jpa.properties.hibernate.show_sql=true",
      "spring.jpa.properties.hibernate.format_sql=true"
   ]
)
class MySqlUpsertTest {
   @Autowired
   lateinit var movieRepository: MovieRepository

   lateinit var connectionRegistry: InMemoryJdbcConnectionRegistry
   lateinit var connectionFactory: JdbcConnectionFactory


   companion object {
      @JvmField
      @Container
      @ServiceConnection
      val mysqlContainer = MySQLContainer("mysql:8.4.0")
   }

   @BeforeEach
   fun setup() {
      mysqlContainer.waitingFor(Wait.forListeningPort())
      val connectionDetails = JdbcUrlCredentialsConnectionConfiguration(
         "movies",
         JdbcDriver.MYSQL,
         JdbcUrlAndCredentials(
            mysqlContainer.getJdbcUrl(),
            mysqlContainer.username,
            mysqlContainer.password
         )
      )
      connectionRegistry =
         InMemoryJdbcConnectionRegistry(listOf(connectionDetails))
      connectionFactory = HikariJdbcConnectionFactory(connectionRegistry, HikariConfig())
   }

   @AfterEach
   fun cleanupDatabase() {
      try {
         // Establish a connection to the MySQL database
         val connection = DriverManager.getConnection(mysqlContainer.getJdbcUrl(), mysqlContainer.getUsername(), mysqlContainer.getPassword())

         // Create a statement
         val statement = connection.createStatement()

         // Execute the DROP TABLE statements
         statement.execute("DROP TABLE IF EXISTS film")
         statement.execute("DROP TABLE IF EXISTS movie")

         // Close the statement and connection
         statement.close()
         connection.close()
      } catch (e: SQLException) {
         // Handle any exceptions that occur during the database operations
         e.printStackTrace()
      }
   }

   fun vyne() = testVyne(
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
   ) { schema -> listOf(JdbcInvoker(connectionFactory, SimpleSchemaProvider(schema))) }

   @Test
   fun `can use a TaxiQL statement to insert a row`(): Unit = runBlocking {
      val result = vyne().query(
         """
         given { movie : Film = { ID : null , TITLE : "A New Hope" } }
         call MovieDb::insertMovie
         """.trimIndent()
      )
         .typedObjects()

      // See MySQLDbSupport ... upserts don't return values.
      // So, re-query to validate
      val queryResult = vyne().query("""find { Film[] }""").typedObjects()
      queryResult.shouldHaveSize(1)
      queryResult.single()["ID"].shouldNotBeNull()
   }

   @Test
   fun `can use a TaxiQL statement to update a row`(): Unit = runBlocking {
      val vyne = vyne()
      val insert = vyne.query(
         """
         given { movie : Film = { ID : null , TITLE : "A New Hope" } }
         call MovieDb::insertMovie
         """.trimIndent()
      )
         .typedObjects()

      val postInsertQuery = vyne.query("""find { Film[] }""")
         .typedObjects()

      postInsertQuery.should.have.size(1)
      val id = postInsertQuery.single()["ID"].shouldNotBeNull().value as Int

      val update = vyne.query(
         """
         given { movie : Film = { ID : $id , TITLE : "Star Wars IV - A New Hope" } }
         call MovieDb::updateMovie
         """.trimIndent()
      )
         .typedObjects()

      val postUpdateQuery = vyne.query("""find { Film[] }""")
         .typedObjects()

      postUpdateQuery.should.have.size(1)

      postUpdateQuery.single().toRawObject()
         .shouldBe(
            mapOf(
               "ID" to id,
               "TITLE" to "Star Wars IV - A New Hope"
            )
         )
   }

   @Test
   fun `can insert a batch of rows`(): Unit = runBlocking {
      val vyne = vyne()

      // can't query here, as the table doesn't exist
//      vyne.query("""find { Film[] }""")
//         .typedObjects()
//         .shouldBeEmpty()

      val result = vyne.query(
         """
         given { movies:Film[] = [ { TITLE : "Star Wars" } , { TITLE : "Back to the Future" } ] }
         call MovieDb::insertFilms
         """.trimIndent()
      )
         .rawObjects()
      // MySQL / jOOQ don't support Update...returning
      // https://github.com/jOOQ/jOOQ/issues/6865
      // Therefore, we need to re-query to find the rows
//      result.forEach { it["ID"].shouldNotBeNull() }
      val insertedFilms = vyne.query("""find { Film[] }""")
         .typedObjects()

    insertedFilms.shouldHaveSize(2)
   }

}
