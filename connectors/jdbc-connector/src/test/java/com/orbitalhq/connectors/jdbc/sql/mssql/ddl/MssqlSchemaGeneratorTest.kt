package com.orbitalhq.connectors.jdbc.sql.mssql.ddl

import com.orbitalhq.connectors.config.jdbc.JdbcUrlAndCredentials
import com.orbitalhq.connectors.config.jdbc.JdbcUrlCredentialsConnectionConfiguration
import com.orbitalhq.connectors.jdbc.DatabaseMetadataService
import com.orbitalhq.connectors.jdbc.HikariJdbcConnectionFactory
import com.orbitalhq.connectors.jdbc.JdbcConnectionFactory
import com.orbitalhq.connectors.jdbc.JdbcTable
import com.orbitalhq.connectors.jdbc.TableTaxiGenerationRequest
import com.orbitalhq.connectors.jdbc.registry.InMemoryJdbcConnectionRegistry
import com.orbitalhq.connectors.jdbc.schema.builtInSchema
import com.zaxxer.hikari.HikariConfig
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.jdbc.core.JdbcTemplate
import org.testcontainers.containers.MSSQLServerContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
class MssqlSchemaGeneratorTest {


   companion object {
      @JvmField
      @Container
      @ServiceConnection
      val mssqlContainer = MSSQLServerContainer("mcr.microsoft.com/mssql/server")
         .acceptLicense()
         .withInitScript("mssql/northwind-schema.sql")
   }

   lateinit var connectionDetails:JdbcUrlCredentialsConnectionConfiguration
   lateinit var connectionRegistry: InMemoryJdbcConnectionRegistry
   lateinit var connectionFactory: JdbcConnectionFactory
   lateinit var jdbcTemplate: JdbcTemplate

   @BeforeEach
   fun setup() {
      mssqlContainer.waitingFor(Wait.forListeningPort())
      connectionDetails = JdbcUrlCredentialsConnectionConfiguration(
         "northwind",
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
      jdbcTemplate = connectionFactory.jdbcTemplate("northwind").jdbcTemplate
   }

   val schema = builtInSchema

   @Test
   fun `can list tables`() {
      val tables = DatabaseMetadataService(jdbcTemplate, connectionDetails)
         .listTables()

      tables.map { it.tableName }
         .shouldContain("Customers")
   }
   @Test
   fun `can generate MSSQL schema`() {

      val generatedTaxi = DatabaseMetadataService(jdbcTemplate, connectionDetails)
         .generateTaxi(
            listOf(
               TableTaxiGenerationRequest(
                  JdbcTable(
                     "Northwind",
                     "Customers"
                  )
               )
            ),
            schema, "mssqlNorthwind"
         )
      generatedTaxi.shouldNotBeNull()
   }
}
