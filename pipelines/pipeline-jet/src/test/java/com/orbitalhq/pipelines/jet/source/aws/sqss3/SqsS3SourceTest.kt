package com.orbitalhq.pipelines.jet.source.aws.sqss3

import com.winterbe.expekt.should
import com.orbitalhq.VersionedTypeReference
import com.orbitalhq.connectors.aws.core.registry.AwsInMemoryConnectionRegistry
import com.orbitalhq.connectors.jdbc.JdbcConnectionFactory
import com.orbitalhq.connectors.jdbc.SqlUtils
import com.orbitalhq.connectors.jdbc.registry.InMemoryJdbcConnectionRegistry
import com.orbitalhq.pipelines.jet.*
import com.orbitalhq.pipelines.jet.api.transport.PipelineSpec
import com.orbitalhq.pipelines.jet.api.transport.aws.sqss3.AwsSqsS3TransportInputSpec
import com.orbitalhq.pipelines.jet.api.transport.http.CronExpressions
import com.orbitalhq.pipelines.jet.api.transport.jdbc.JdbcTransportOutputSpec
import com.orbitalhq.schemas.Type
import org.awaitility.Awaitility
import org.jooq.Asterisk
import org.jooq.DSLContext
import org.jooq.impl.DSL
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
@com.orbitalhq.formats.Csv(
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
      val testSetup = jetWithSpringAndVyne(
         coinBaseSchema,
         emptyList(),
         listOf(localstack.awsConnection()),
         UTCClockProvider::class.java
      )
      testSetup.applicationContext.getBean(AwsInMemoryConnectionRegistry::class.java)
         .register(localstack.awsConnection())
      // Register the connection so we can look it up later
      val connectionRegistry = testSetup.applicationContext.getBean(InMemoryJdbcConnectionRegistry::class.java)
      connectionRegistry.register(postgresSQLContainerFacade.connection)
      val (listSinkTarget, outputSpec) = listSinkTargetAndSpec(
         testSetup.applicationContext,
         targetType = "OrderWindowSummary"
      )
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

      startPipeline(testSetup.hazelcastInstance, testSetup.vyneClient, pipelineSpec)

      val connectionFactory = testSetup.applicationContext.getBean(JdbcConnectionFactory::class.java)

      waitForRowCount(
         connectionFactory.dsl(postgresSQLContainerFacade.connection),
         testSetup.schema.type("OrderWindowSummary"),
         1,
         duration = Duration.ofSeconds(30)
      )
      symbols(connectionFactory.dsl(postgresSQLContainerFacade.connection), testSetup.schema.type("OrderWindowSummary"))
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
         dsl.select(DSL.asterisk())
            .from(DSL.name(SqlUtils.tableNameOrTypeName(type.taxiType)))
            .fetchMaps()
            .map { record ->
               record["symbol"].toString()
            }

      } catch (e: Exception) {
         emptyList<String>()
      } // return -1 if the table doesn't exist
   }

}
