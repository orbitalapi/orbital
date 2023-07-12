package io.vyne.pipelines.jet.sink.jdbc

import io.vyne.connectors.config.jdbc.DefaultJdbcConnectionConfiguration
import io.vyne.connectors.config.jdbc.JdbcDriver
import io.vyne.connectors.jdbc.buildUrlAndCredentials
import io.vyne.connectors.jdbc.builders.SnowflakeJdbcUrlBuilder
import io.vyne.pipelines.jet.BaseJetIntegrationTest
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.jdbc.JdbcTransportOutputSpec
import io.vyne.pipelines.jet.pipelines.PostgresDdlGenerator
import io.vyne.pipelines.jet.queueOf
import io.vyne.pipelines.jet.source.fixed.FixedItemsSourceSpec
import io.vyne.schemas.fqn
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Testcontainers
import java.sql.DriverManager
import java.sql.ResultSet
import kotlin.test.assertEquals


@Ignore("Needs a snowflake instance - use to test locally")
@Testcontainers
@RunWith(SpringRunner::class)
class JdbcSnowflakeSinkTest : BaseJetIntegrationTest() {


   lateinit var database: String
   lateinit var username: String
   lateinit var password: String
   lateinit var host: String
   lateinit var port: String

   @Rule
   @JvmField
   val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:11.1") as PostgreSQLContainer<*>

   @Before
   fun before() {
      postgreSQLContainer.start()
      postgreSQLContainer.waitingFor(Wait.forListeningPort())

      port = postgreSQLContainer.firstMappedPort.toString()
      username = postgreSQLContainer.username
      password = postgreSQLContainer.password
      database = postgreSQLContainer.databaseName
      host = postgreSQLContainer.host

   }

   @Test
   fun canOutputToSnowflake() {

      val connection = DefaultJdbcConnectionConfiguration.forParams(
         "test-connection",
         JdbcDriver.SNOWFLAKE,
         connectionParameters = mapOf(
            SnowflakeJdbcUrlBuilder.Parameters.ACCOUNT to "hw62117.eu-west-1",
            SnowflakeJdbcUrlBuilder.Parameters.USERNAME to "anthonycowan",
            SnowflakeJdbcUrlBuilder.Parameters.PASSWORD to "EYE57glass!!",
            SnowflakeJdbcUrlBuilder.Parameters.DATABASE to "DEMO_DB",
            SnowflakeJdbcUrlBuilder.Parameters.WAREHOUSE_NAME to "COMPUTE_WH",
            SnowflakeJdbcUrlBuilder.Parameters.SCHEMA_NAME to "PUBLIC",
            SnowflakeJdbcUrlBuilder.Parameters.ROLE to "SYSADMIN",
         )
      )

      // Pipeline Jdbc -> Direct
      val schemaSource = """
         model Person {
            firstName : FirstName inherits String
            lastName : LastName inherits String
         }
         model Target {
            givenName : FirstName
         }
      """
      val testSetup = jetWithSpringAndVyne(
         schemaSource, listOf(connection)
      )
      val pipelineSpec = PipelineSpec(
         name = "test-http-poll",
         input = FixedItemsSourceSpec(
            items = queueOf("""{ "firstName" : "jimmy", "lastName" : "Schmitt" }"""),
            typeName = "Person".fqn()
         ),
         outputs = listOf(
            JdbcTransportOutputSpec(
               "test-connection",
               "Target"
            )
         )
      )
      startPipeline(testSetup.hazelcastInstance, testSetup.vyneClient, pipelineSpec)

      Thread.sleep(10000)

      val postgresDdlGenerator = PostgresDdlGenerator()
      val targetTable = postgresDdlGenerator.generateDdl(
         testSetup.schema.versionedType(pipelineSpec.outputs.first().targetType!!.typeName),
         testSetup.schema
      )

      val urlCredentials = connection.buildUrlAndCredentials()
      val url = connection.buildUrlAndCredentials().url

      val databaseConnection = DriverManager.getConnection(url, urlCredentials.username, urlCredentials.password)
      val statement = databaseConnection.createStatement()
      val rs: ResultSet = statement.executeQuery("select * from ${targetTable.generatedTableName}")
      while (rs.next()) {
         val givenName =  rs.getString("givenName")
         assertEquals("jimmy", givenName)
      }
      rs.close()

   }
}
