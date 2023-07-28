package io.vyne.connectors.jdbc.schema

import com.google.common.io.Resources
import io.vyne.connectors.jdbc.DatabaseMetadataService
import io.vyne.connectors.jdbc.JdbcConnectorTaxi
import io.vyne.connectors.config.jdbc.JdbcDriver
import io.vyne.connectors.config.jdbc.JdbcUrlAndCredentials
import io.vyne.connectors.config.jdbc.JdbcUrlCredentialsConnectionConfiguration
import io.vyne.connectors.jdbc.SimpleJdbcConnectionFactory
import io.vyne.connectors.jdbc.TableTaxiGenerationRequest
import io.vyne.query.VyneQlGrammar
import lang.taxi.testing.TestHelpers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Testcontainers

@Suppress("UnstableApiUsage")
@Testcontainers
class ComplexTaxiSchemaGeneratorTest {

   lateinit var jdbcUrl: String
   lateinit var username: String
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
      val template = SimpleJdbcConnectionFactory()
         .jdbcTemplate(connectionDetails)
      val metadataService = DatabaseMetadataService(template.jdbcTemplate)
      val tablesToGenerate = metadataService.listTables().map {
         TableTaxiGenerationRequest(it)
      }
      val generatedTaxiCode = metadataService.generateTaxi(
         tables = tablesToGenerate,
         schema = builtInSchema,
         connectionName = "testConnection"
      )
      val expected = Resources.getResource("postgres/pagila-expected.taxi")
         .readText()
      val builtInTypes = listOf(VyneQlGrammar.QUERY_TYPE_TAXI, JdbcConnectorTaxi.schema).joinToString("\n")
      TestHelpers.expectToCompileTheSame(generatedTaxiCode.taxi + builtInTypes, builtInTypes + expected)
   }
}

