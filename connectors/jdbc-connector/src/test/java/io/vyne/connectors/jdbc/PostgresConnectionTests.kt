package io.vyne.connectors.jdbc

import com.winterbe.expekt.should
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
class PostgresConnectionTests {

   lateinit var jdbcUrl: String;
   lateinit var username: String;
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
   fun `can list metadata from postgres database`() {
      val connectionDetails = JdbcUrlCredentialsConnectionConfiguration(
         "postgres",
         JdbcDriver.POSTGRES,
         JdbcUrlAndCredentials(jdbcUrl, username, password)
      )
      val template = DefaultJdbcTemplateProvider(connectionDetails)
         .build()
      val metadataService = DatabaseMetadataService(template.jdbcTemplate)
      metadataService.testConnection().should.be.`true`
      val tables = metadataService.listTables()
      tables.should.have.size(1)
      tables.should.contain(JdbcTable("public","actor"))

      val columns = metadataService.listColumns("public","actor")
      columns.should.have.size(4)
   }
}
