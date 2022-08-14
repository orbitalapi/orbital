package io.vyne.pipelines.jet.sink.redshift

import io.vyne.connectors.jdbc.DefaultJdbcConnectionConfiguration
import io.vyne.connectors.jdbc.JdbcDriver
import io.vyne.connectors.jdbc.builders.RedshiftJdbcUrlBuilder
import io.vyne.pipelines.jet.BaseJetIntegrationTest
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.redshift.RedshiftTransportOutputSpec
import io.vyne.pipelines.jet.pipelines.PostgresDdlGenerator
import io.vyne.pipelines.jet.queueOf
import io.vyne.pipelines.jet.source.fixed.FixedItemsSourceSpec
import io.vyne.schemas.fqn
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Testcontainers
import java.sql.DriverManager
import java.sql.ResultSet
import kotlin.test.assertEquals


@Testcontainers
@RunWith(SpringRunner::class)
//This test will not work until redshift in localstack support connecting over JDBC!
//Instead fire up a real cluster via AWS UI/CLI, make it publicaly accessible
@Ignore
class RedshiftSinkTest : BaseJetIntegrationTest() {


   lateinit var database: String
   lateinit var username: String
   lateinit var password: String
   lateinit var host: String
   lateinit var port: String

   @Rule
   @JvmField
   val localStackContainer = LocalStackContainer("0.11.3").withServices(LocalStackContainer.Service.REDSHIFT)


   @Before
   fun before() {
        localStackContainer.start()
        localStackContainer.waitingFor(Wait.forListeningPort())

       port = localStackContainer.firstMappedPort.toString()
       username = "user"
       password = ""
       database = "localStackContainer.databaseName"
       host = localStackContainer.host

   }

   //This test will not work until redshift in localstack support connecting over JDBC!
   //Instead fire up a real cluster via AWS UI/CLI, make it publicaly accessible
   @Ignore
   fun canOutputToRedshift() {

      val connection = DefaultJdbcConnectionConfiguration.forParams(
         "test-connection",
         JdbcDriver.REDSHIFT,
         connectionParameters = mapOf(

            RedshiftJdbcUrlBuilder.Parameters.HOST to "redshift-cluster-1.cofg7uy6ctz3.eu-west-1.redshift.amazonaws.com",
            RedshiftJdbcUrlBuilder.Parameters.PORT to "5439",
            RedshiftJdbcUrlBuilder.Parameters.DATABASE to "dev",
            RedshiftJdbcUrlBuilder.Parameters.USERNAME to "awsuser",
            RedshiftJdbcUrlBuilder.Parameters.PASSWORD to "SET TO CLUSTER PASSWORD",
         )
      )

      // Pipeline Redshift -> Direct
      val schemaSource = """
         model Person {
            firstName : FirstName inherits String
            lastName : LastName inherits String
         }
         model Target {
            givenName : FirstName
         }
      """
      val (jetInstance, _, vyneProvider) = jetWithSpringAndVyne(
         schemaSource, listOf(connection)
      )
      val pipelineSpec = PipelineSpec(
         name = "test-http-poll",
         input = FixedItemsSourceSpec(
            items = queueOf("""{ "firstName" : "jimmy", "lastName" : "Schmitt" }"""),
            typeName = "Person".fqn()
         ),
         output = RedshiftTransportOutputSpec(
            "test-connection",
            "Target"
         )
      )
      startPipeline(jetInstance, vyneProvider, pipelineSpec)

      Thread.sleep(10000)

      val postgresDdlGenerator = PostgresDdlGenerator()
      val schema = vyneProvider.createVyne().schema
      val targetTable = postgresDdlGenerator.generateDdl(schema.versionedType(pipelineSpec.output.targetType.typeName), schema)

      val urlCredentials = connection.buildUrlAndCredentials()
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
