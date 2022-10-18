package io.vyne.pipelines.jet.source.aws.sqss3

import com.winterbe.expekt.should
import io.vyne.VersionedTypeReference
import io.vyne.connectors.aws.core.registry.AwsConnectionRegistry
import io.vyne.connectors.jdbc.JdbcConnectionFactory
import io.vyne.connectors.jdbc.SqlUtils
import io.vyne.connectors.jdbc.registry.JdbcConnectionRegistry
import io.vyne.pipelines.jet.BaseJetIntegrationTest
import io.vyne.pipelines.jet.PostgresSQLContainerFacade
import io.vyne.pipelines.jet.UTCClockProvider
import io.vyne.pipelines.jet.api.transport.PipelineSpec
import io.vyne.pipelines.jet.api.transport.aws.sqss3.AwsSqsS3TransportInputSpec
import io.vyne.pipelines.jet.api.transport.http.CronExpressions
import io.vyne.pipelines.jet.api.transport.jdbc.JdbcTransportOutputSpec
import io.vyne.pipelines.jet.awsConnection
import io.vyne.pipelines.jet.populateS3AndSqs
import io.vyne.schemas.Type
import org.awaitility.Awaitility
import org.jooq.DSLContext
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.test.context.junit4.SpringRunner
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.Duration
import java.time.Instant

@Testcontainers
@RunWith(SpringRunner::class)
class SqsS3SourceTest : BaseJetIntegrationTest() {
   private val localStackImage: DockerImageName = DockerImageName.parse("localstack/localstack").withTag("1.0.4")
   private val bucket = "testbucket"
   private val objectKey = "myfile"
   private val sqsQueueName = "testqueue"
   private var sqsQueueUrl = ""

   @JvmField
   @Rule
   var localstack: LocalStackContainer = LocalStackContainer(localStackImage)
      .withServices(LocalStackContainer.Service.S3, LocalStackContainer.Service.SQS)

   lateinit var postgresSQLContainerFacade: PostgresSQLContainerFacade

   @Rule
   @JvmField
   val postgreSQLContainer = PostgreSQLContainer<Nothing>("postgres:11.1") as PostgreSQLContainer<*>



   @Before
   fun setUp() {
      postgresSQLContainerFacade = PostgresSQLContainerFacade(postgreSQLContainer)
      postgresSQLContainerFacade.start()
      sqsQueueUrl = populateS3AndSqs(localstack, bucket, objectKey, sqsQueueName)
   }

   @Test
   fun `can read a csv file from s3`() {
      // Pipeline S3 -> Direct
      // Date,Symbol,Open,High,Low,Close,Volume BTC,Volume USD
      val coinBaseSchema = """
type alias Price as Decimal
type alias Symbol as String
@io.vyne.formats.Csv(
            delimiter = ",",
            nullValue = "NULL"
         )
type OrderWindowSummary {
    symbol : Symbol by column("Symbol")
    open : Price by column("Open")
    // Added column
    high : Price by column("High")
    // Changed column
    close : Price by column("Close")
}""".trimIndent()
      val (hazelcastInstance, applicationContext, vyneProvider) = jetWithSpringAndVyne(
         coinBaseSchema,
         emptyList(),
         listOf(localstack.awsConnection()),
         UTCClockProvider::class.java
      )
      applicationContext.getBean(AwsConnectionRegistry::class.java).register(localstack.awsConnection())
      // Register the connection so we can look it up later
      val connectionRegistry = applicationContext.getBean(JdbcConnectionRegistry::class.java)
      connectionRegistry.register(postgresSQLContainerFacade.connection)
      val (listSinkTarget, outputSpec) = listSinkTargetAndSpec(applicationContext, targetType = "OrderWindowSummary")
      val pipelineSpec = PipelineSpec(
         name = "aws-s3-source",
         input = AwsSqsS3TransportInputSpec(
            localstack.awsConnection().connectionName,
            VersionedTypeReference.parse("OrderWindowSummary"),
            queueName = sqsQueueUrl,
            pollSchedule = CronExpressions.EVERY_TEN_SECONDS
         ),
         outputs = listOf(
            JdbcTransportOutputSpec(
               "test-connection",
               "OrderWindowSummary"
            )
         )
      )

      startPipeline(hazelcastInstance, vyneProvider, pipelineSpec)

      val connectionFactory = applicationContext.getBean(JdbcConnectionFactory::class.java)
      val vyne = vyneProvider.createVyne()

      waitForRowCount(
         connectionFactory.dsl(postgresSQLContainerFacade.connection),
         vyne.type("OrderWindowSummary"),
         1,
         duration = Duration.ofSeconds(30)
      )
      symbols(connectionFactory.dsl(postgresSQLContainerFacade.connection), vyne.type("OrderWindowSummary"))
         .should.elements("BTCUSD", "")
   }

   private fun waitForRowCount(
      dsl: DSLContext,
      type: Type,
      rowCount: Int,
      startTime: Instant = Instant.now(),
      duration: Duration = Duration.ofSeconds(10)
   ) {
      Awaitility.await().atMost(duration)
         .until {
            val listOfSymbols = symbols(dsl, type)
            logger.info(
               "Row count after ${
                  Duration.between(startTime, Instant.now()).toMillis()
               }ms is ${listOfSymbols.size} (Waiting until it hits $rowCount)"
            )
            listOfSymbols.size >= rowCount
         }
   }

   private fun symbols(dsl: DSLContext, type: Type): List<String> {
      return try {
         dsl.fetch("select * from ${SqlUtils.tableNameOrTypeName(type.taxiType)}").map { record ->
            record["symbol"].toString()
         }

      } catch (e: Exception) {
         emptyList<String>()
      } // return -1 if the table doesn't exist
   }

}
