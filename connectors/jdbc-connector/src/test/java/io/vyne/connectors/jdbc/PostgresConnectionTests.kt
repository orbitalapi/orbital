package io.vyne.connectors.jdbc

import com.winterbe.expekt.should
import io.vyne.connectors.ConnectionSucceeded
import io.vyne.utils.get
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
class PostgresConnectionTests {

   lateinit var jdbcUrl: String
   lateinit var username: String
   lateinit var password: String

   @Rule
   @JvmField
   val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:11.1")
      .withInitScript("postgres/actor-schema.sql") as PostgreSQLContainer<*>

   @Before
   fun before() {
      postgreSQLContainer.start()
      postgreSQLContainer.waitingFor(Wait.forListeningPort())

      jdbcUrl = postgreSQLContainer.jdbcUrl
      username = postgreSQLContainer.username
      password = postgreSQLContainer.password

   }

   @Test
   fun `testing connection with valid credentials returns true`() {
      val connectionDetails = JdbcUrlCredentialsConnectionConfiguration(
         "postgres",
         JdbcDriver.POSTGRES,
         JdbcUrlAndCredentials(jdbcUrl, username, password)
      )
      val template = SimpleJdbcConnectionFactory()
         .jdbcTemplate(connectionDetails)
      val metadataService = DatabaseMetadataService(template.jdbcTemplate)
      metadataService.testConnection(JdbcDriver.POSTGRES.metadata.testQuery).get().should.equal(ConnectionSucceeded)
   }

   @Test
   fun `testing connection with valid location but invalid credentials returns false`() {
      val connectionDetails = JdbcUrlCredentialsConnectionConfiguration(
         "postgres",
         JdbcDriver.POSTGRES,
         JdbcUrlAndCredentials(jdbcUrl, "wrongUser", "wrongPassword")
      )
      val template = SimpleJdbcConnectionFactory()
         .jdbcTemplate(connectionDetails)
      val metadataService = DatabaseMetadataService(template.jdbcTemplate)
      metadataService.testConnection(JdbcDriver.POSTGRES.metadata.testQuery)
         .get().should.equal("Failed to obtain JDBC Connection; nested exception is org.postgresql.util.PSQLException: FATAL: password authentication failed for user \"wrongUser\"")
   }

   @Test
   fun `testing connection with invalid location returns false`() {
      val connectionDetails = JdbcUrlCredentialsConnectionConfiguration(
         "postgres",
         JdbcDriver.POSTGRES,
         JdbcUrlAndCredentials("jdbc:postgresql://wronghost:9999", "wrongUser", "wrongPassword")
      )
      val template = SimpleJdbcConnectionFactory()
         .jdbcTemplate(connectionDetails)
      val metadataService = DatabaseMetadataService(template.jdbcTemplate)
      metadataService.testConnection(JdbcDriver.POSTGRES.metadata.testQuery)
         .get().toString()
         .trim().should.equal("Failed to obtain JDBC Connection; nested exception is org.postgresql.util.PSQLException: Unable to parse URL")
   }

   @Test
   fun `can list metadata from postgres database`() {
      val connectionDetails = JdbcUrlCredentialsConnectionConfiguration(
         "postgres",
         JdbcDriver.POSTGRES,
         JdbcUrlAndCredentials(jdbcUrl, username, password)
      )
      val template = SimpleJdbcConnectionFactory()
         .jdbcTemplate(connectionDetails)
      val metadataService = DatabaseMetadataService(template.jdbcTemplate)
      metadataService.testConnection(JdbcDriver.POSTGRES.metadata.testQuery).get().should.equal(ConnectionSucceeded)
      val tables = metadataService.listTables()
      tables.should.have.size(1)
      tables.should.contain(JdbcTable("public", "actor",
         listOf(
            JdbcColumn(
               "actor_id",
               "int4",
               10,
               0,
               false
            )
         ), listOf(JdbcIndex(
         "actor_pkey",
         listOf(
            JdbcColumn(
               "actor_id",
               "int4",
               10,
               0,
               false
            )
         )
      ))))

      val columns = metadataService.listColumns("public", "actor")
      columns.should.have.size(4)
   }
}
