package io.vyne.connectors.jdbc.schema

import com.google.common.io.Resources
import io.vyne.connectors.jdbc.DatabaseMetadataService
import io.vyne.connectors.jdbc.DefaultJdbcTemplateProvider
import io.vyne.connectors.jdbc.JdbcDriver
import io.vyne.connectors.jdbc.JdbcUrlAndCredentials
import io.vyne.connectors.jdbc.JdbcUrlCredentialsConnectionConfiguration
import lang.taxi.testing.TestHelpers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
class ComplexTaxiSchemaGeneratorTest {

   lateinit var jdbcUrl: String;
   lateinit var username: String;
   lateinit var password: String

   @Rule
   @JvmField
   val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:11.1")
      .withInitScript("postgres/pagila-schema.sql") as PostgreSQLContainer<*>

   @Before
   fun before() {
      postgreSQLContainer.start()
      postgreSQLContainer.waitingFor(Wait.forListeningPort())

      jdbcUrl = postgreSQLContainer.jdbcUrl
      username = postgreSQLContainer.username
      password = postgreSQLContainer.password

   }

   @Test
   fun `generates complex schema`() {
      val connectionDetails = JdbcUrlCredentialsConnectionConfiguration(
         "postgres",
         JdbcDriver.POSTGRES,
         JdbcUrlAndCredentials(jdbcUrl, username, password)
      )
      val template = DefaultJdbcTemplateProvider(connectionDetails)
         .build()
      val metadataService = DatabaseMetadataService(template.jdbcTemplate)
      val tablesToGenerate = metadataService.listTables()
      val taxi = metadataService.generateTaxi(tables = tablesToGenerate, namespace = "io.vyne.test")
      val expected = Resources.getResource("postgres/pagila-expected.taxi")
         .readText()
      TestHelpers.expectToCompileTheSame(taxi, expected)
   }
}

