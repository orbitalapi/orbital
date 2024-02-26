package com.orbitalhq.connectors.jdbc.sql.mssql.ddl

import com.orbitalhq.connectors.config.jdbc.JdbcDriver
import com.orbitalhq.connectors.config.jdbc.JdbcUrlAndCredentials
import com.orbitalhq.connectors.config.jdbc.JdbcUrlCredentialsConnectionConfiguration
import com.orbitalhq.connectors.jdbc.DatabaseMetadataService
import com.orbitalhq.connectors.jdbc.JdbcConnectionFactory
import com.orbitalhq.connectors.jdbc.SimpleJdbcConnectionFactory
import com.orbitalhq.connectors.jdbc.drivers.databaseSupport
import com.orbitalhq.connectors.jdbc.drivers.mssql.MssqlJdbcUrlBuilder
import com.orbitalhq.connectors.jdbc.sql.ddl.TableGenerator
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.utils.withoutWhitespace
import com.winterbe.expekt.should
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.containers.MSSQLServerContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.random.Random

@Testcontainers
class MssqlTableGeneratorTest {

   lateinit var connectionDetails: JdbcUrlCredentialsConnectionConfiguration
   lateinit var connectionFactory: JdbcConnectionFactory

   companion object {
      @JvmField
      @Container
      @ServiceConnection
      val mssqlContainer = MSSQLServerContainer("mcr.microsoft.com/mssql/server")
         .acceptLicense()
         .withInitScript("mssql/northwind-schema.sql")
   }

   @BeforeEach
   fun setup() {
      mssqlContainer.waitingFor(Wait.forListeningPort())
      connectionDetails = JdbcUrlCredentialsConnectionConfiguration(
         "mssql",
         JdbcDriver.MSSQL,
         JdbcUrlAndCredentials(
            "${mssqlContainer.getJdbcUrl()};Database=Northwind",
            mssqlContainer.username,
            mssqlContainer.password
         ),
         params = mapOf(
            MssqlJdbcUrlBuilder.Parameters.SCHEMA to "dbo",
            MssqlJdbcUrlBuilder.Parameters.DATABASE to "Northwind",
         )
      )
      connectionFactory = SimpleJdbcConnectionFactory()
   }


   @Test
   fun `can create table with auto incrementing numeric id`() {
      val tableName = "Person_" + Random.nextInt(0, 999999)
      val schema = TaxiSchema.from(
         """
         @com.orbitalhq.jdbc.Table(schema = "dbo", table = "$tableName", connection = "mssql")
         model Person {
            @Id @GeneratedId
            id : PersonId inherits Int
            firstName : FirstName inherits String
            lastName : LastName inherits String
         }
      """.trimIndent()
      )
      val ddl = TableGenerator(schema, connectionDetails.databaseSupport)
         .generate(schema.type("Person"), connectionFactory.dsl(connectionDetails))
      ddl.ddlStatement.sql.withoutWhitespace().shouldBe(
         """begin try create table [$tableName] (
            [id] int identity(1, 1) not null,
            [firstName] varchar(max) not null,
            [lastName] varchar(max) not null,
            constraint [$tableName-pk] primary key ([id])
            ) end try begin catch if error_number() != 2714 throw; end catch
""".withoutWhitespace()
      )
   }

   @Test
   fun `can create mssql table`() {
      val tableName = "Person_" + Random.nextInt(0, 999999)
      val schema = TaxiSchema.from(
         """
         @com.orbitalhq.jdbc.Table(schema = "dbo", table = "$tableName", connection = "mssql")
         model Person {
            @Id
            id : PersonId inherits Int
            firstName : FirstName inherits String
            lastName : LastName inherits String
            favouriteColor : String?
            age : Age inherits Int
            fullName : FullName inherits String by concat(this.firstName, ' ', this.lastName)
         }
      """.trimIndent()
      )
      TableGenerator(schema, connectionDetails.databaseSupport).execute(
         schema.type("Person"),
         connectionFactory.dsl(connectionDetails)
      )

      val template = SimpleJdbcConnectionFactory()
         .jdbcTemplate(connectionDetails)
      val metadataService = DatabaseMetadataService(template.jdbcTemplate, connectionDetails)
      val tables = metadataService.listTables()
      val createdTable = tables.firstOrNull { it.tableName == tableName } ?: error("Failed to create $tableName")
      val columns = metadataService.listColumns("dbo", createdTable.tableName)
      columns.should.have.size(6)
      createdTable.constrainedColumns.should.have.size(1)
      columns.single { it.columnName == "favouriteColor" }.nullable.should.be.`true`
      columns.single { it.columnName == "firstName" }.nullable.should.be.`false`
      columns.single { it.columnName == "age" }.dataType.should.equal("int")
      createdTable.indexes.should.have.size(1)
   }
}
