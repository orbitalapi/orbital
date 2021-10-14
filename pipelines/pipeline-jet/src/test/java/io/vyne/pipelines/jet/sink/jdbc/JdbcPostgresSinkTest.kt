package io.vyne.pipelines.jet.sink.redshift

import io.vyne.connectors.jdbc.DefaultJdbcConnectionConfiguration
import io.vyne.connectors.jdbc.JdbcDriver
import io.vyne.connectors.jdbc.builders.PostgresJdbcUrlBuilder
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.redshift.JdbcTransportOutputSpec
import io.vyne.pipelines.jet.pipelines.PostgresDdlGenerator
import io.vyne.pipelines.jet.queueOf
import io.vyne.pipelines.jet.source.fixed.FixedItemsSourceSpec
import io.vyne.pipelines.jet.source.kafka.AbstractKafkaJetTest
import io.vyne.schemas.fqn
import org.junit.Before
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


@Testcontainers
@RunWith(SpringRunner::class)
class JdbcPostgresSinkTest : AbstractKafkaJetTest() {


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
   fun canOutputToJdbc() {

      val connection = DefaultJdbcConnectionConfiguration.forParams(
         "test-connection",
         JdbcDriver.POSTGRES,
         connectionParameters = mapOf(
            PostgresJdbcUrlBuilder.Parameters.HOST to host,
            PostgresJdbcUrlBuilder.Parameters.PORT to port,
            PostgresJdbcUrlBuilder.Parameters.DATABASE to database,
            PostgresJdbcUrlBuilder.Parameters.USERNAME to username,
            PostgresJdbcUrlBuilder.Parameters.PASSWORD to password,
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
      val (jetInstance, applicationContext, vyneProvider) = jetWithSpringAndVyne(
         schemaSource, listOf(connection)
      )
      val pipelineSpec = PipelineSpec(
         name = "test-http-poll",
         input = FixedItemsSourceSpec(
            items = queueOf("""{ "firstName" : "jimmy", "lastName" : "Schmitt" }"""),
            typeName = "Person".fqn()
         ),
         output = JdbcTransportOutputSpec(
            "test-connection",
            producerProps(),
            "Target"
         )
      )
      val (pipeline, job) = startPipeline(jetInstance, vyneProvider, pipelineSpec)

      Thread.sleep(10000)

      val postgresDdlGenerator = PostgresDdlGenerator()
      val schema = vyneProvider.createVyne().schema
      val targetTable = postgresDdlGenerator.generateDdl(schema.versionedType(pipelineSpec.output.targetType.typeName), schema)

      val urlCredentials = connection.buildUrlAndCredentials();
      val url = connection.buildUrlAndCredentials().url

      val databaseConnection = DriverManager.getConnection(url, urlCredentials.username, urlCredentials.password)
      val statement = databaseConnection.createStatement()
      val rs: ResultSet = statement.executeQuery("select count(*) from ${targetTable.generatedTableName}")
      while (rs.next()) {
         assertEquals(1, rs.getInt("count"))
      }
      rs.close()

   }
}
