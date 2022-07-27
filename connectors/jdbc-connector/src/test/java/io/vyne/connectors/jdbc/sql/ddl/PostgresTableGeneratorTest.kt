package io.vyne.connectors.jdbc.sql.ddl

import com.winterbe.expekt.should
import io.vyne.connectors.jdbc.*
import io.vyne.schemas.taxi.TaxiSchema
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.random.Random

@Testcontainers
class PostgresTableGeneratorTest {
   lateinit var jdbcUrl: String
   lateinit var username: String
   lateinit var password: String
   lateinit var connectionDetails: JdbcUrlCredentialsConnectionConfiguration
   lateinit var connectionFactory: JdbcConnectionFactory

   @Rule
   @JvmField
   val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:11.1") as PostgreSQLContainer<*>

   @Before
   fun before() {
      postgreSQLContainer.start()
      postgreSQLContainer.waitingFor(Wait.forListeningPort())

      jdbcUrl = postgreSQLContainer.jdbcUrl
      username = postgreSQLContainer.username
      password = postgreSQLContainer.password
      connectionDetails = JdbcUrlCredentialsConnectionConfiguration(
         "postgres",
         JdbcDriver.POSTGRES,
         JdbcUrlAndCredentials(jdbcUrl, username, password)
      )
      connectionFactory = SimpleJdbcConnectionFactory()
   }

   @Test
   fun `can create postgres table`() {
      val tableName = "Person_" + Random.nextInt(0, 999999)
      val schema = TaxiSchema.from(
         """
         @io.vyne.jdbc.Table(schema = "public", table = "$tableName", connection = "postgres")
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
      TableGenerator(schema).execute(schema.type("Person"), connectionFactory.dsl(connectionDetails))

      val template = SimpleJdbcConnectionFactory()
         .jdbcTemplate(connectionDetails)
      val metadataService = DatabaseMetadataService(template.jdbcTemplate)
      val tables = metadataService.listTables()
      val createdTable = tables.firstOrNull { it.tableName == tableName } ?: error("Failed to create $tableName")
      val columns = metadataService.listColumns(createdTable.schemaName, createdTable.tableName)
      columns.should.have.size(6)
      createdTable.constrainedColumns.should.have.size(3)
      columns.single { it.columnName == "favouritecolor" }.nullable.should.be.`true`
      columns.single { it.columnName == "firstname" }.nullable.should.be.`false`
      columns.single { it.columnName == "age" }.dataType.should.equal("int4")
      // two indexes one for the primary key another one for fullName through Index annotation.
      createdTable.indexes.should.have.size(2)
      createdTable
         .indexes
         .flatMap { it.columns.map { indexColumn -> indexColumn.columnName } }
         .should.have.elements("fullname", "id", "firstname", "lastname")
   }
}
