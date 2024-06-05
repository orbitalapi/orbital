package com.orbitalhq.connectors.jdbc.sql.mysql.ddl

import com.orbitalhq.connectors.config.jdbc.JdbcDriver
import com.orbitalhq.connectors.config.jdbc.JdbcUrlAndCredentials
import com.orbitalhq.connectors.config.jdbc.JdbcUrlCredentialsConnectionConfiguration
import com.orbitalhq.connectors.jdbc.DatabaseMetadataService
import com.orbitalhq.connectors.jdbc.JdbcConnectionFactory
import com.orbitalhq.connectors.jdbc.SimpleJdbcConnectionFactory
import com.orbitalhq.connectors.jdbc.drivers.databaseSupport
import com.orbitalhq.connectors.jdbc.sql.ddl.TableGenerator
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.utils.withoutWhitespace
import com.winterbe.expekt.should
import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.random.Random

@Testcontainers
class MySqlTableGeneratorTest {

   lateinit var jdbcUrl: String
   lateinit var username: String
   lateinit var password: String
   lateinit var connectionDetails: JdbcUrlCredentialsConnectionConfiguration
   lateinit var connectionFactory: JdbcConnectionFactory

   @Rule
   @JvmField
   val mySqlContainer = MySQLContainer<Nothing>("mysql:8.4.0") as MySQLContainer<*>

   @Before
   fun before() {
      mySqlContainer.start()
      mySqlContainer.waitingFor(Wait.forListeningPort())

      jdbcUrl = mySqlContainer.jdbcUrl
      username = mySqlContainer.username
      password = mySqlContainer.password
      connectionDetails = JdbcUrlCredentialsConnectionConfiguration(
         "mysql",
         JdbcDriver.MYSQL,
         JdbcUrlAndCredentials(jdbcUrl, username, password)
      )
      connectionFactory = SimpleJdbcConnectionFactory()
   }

   @Test
   fun `can create table with auto incrementing numeric id`() {
      val tableName = "Person"
      val schema = TaxiSchema.from(
         """
         @com.orbitalhq.jdbc.Table(schema = "public", table = "$tableName", connection = "postgres")
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
      ddl.ddlStatement.sql.withoutWhitespace().shouldBe("""create table if not exists `Person` (
         `id` int not null auto_increment,
         `firstName` text not null,
         `lastName` text not null,
         constraint `Person-pk` primary key (`id`)
)""".withoutWhitespace())
   }

   @Test
   fun `can create MySql table`() {
      val tableName = "Person_" + Random.nextInt(0, 999999)
      val schema = TaxiSchema.from(
         """
         @com.orbitalhq.jdbc.Table(schema = "public", table = "$tableName", connection = "postgres")
         model Person {
            @Id
            id : PersonId inherits Int
            @Id
            firstName : FirstName inherits String
            @Id
            lastName : LastName inherits String
            favouriteColor : String?
            age : Age inherits Int
            @Index
            fullName : FullName inherits String by concat(this.firstName, ' ', this.lastName)
         }
      """.trimIndent()
      )
      TableGenerator(schema, connectionDetails.databaseSupport).execute(schema.type("Person"), connectionFactory.dsl(connectionDetails))

      val template = SimpleJdbcConnectionFactory()
         .jdbcTemplate(connectionDetails)
      val metadataService = DatabaseMetadataService(template.jdbcTemplate,connectionDetails)
      val tables = metadataService.listTables()
      val createdTable = tables.firstOrNull { it.tableName == tableName } ?: error("Failed to create $tableName")
      val columns = metadataService.listColumns(createdTable.schemaName, createdTable.tableName)
      columns.should.have.size(6)
      createdTable.constrainedColumns.should.have.size(3)
      columns.single { it.columnName == "favouriteColor" }.nullable.should.be.`true`
      columns.single { it.columnName == "firstName" }.nullable.should.be.`false`
      columns.single { it.columnName == "age" }.dataType.should.equal("INT")
      // two indexes one for the primary key another one for fullName through Index annotation.
      createdTable.indexes.should.have.size(2)
      createdTable
         .indexes
         .flatMap { it.columns.map { indexColumn -> indexColumn.columnName } }
         .should.have.elements("fullName", "id", "firstName", "lastName")
   }

}
