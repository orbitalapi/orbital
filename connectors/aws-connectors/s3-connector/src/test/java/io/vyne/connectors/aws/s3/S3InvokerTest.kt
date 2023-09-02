package com.orbitalhq.connectors.aws.s3

import com.google.common.io.Resources
import com.jayway.awaitility.Awaitility
import com.winterbe.expekt.should
import com.orbitalhq.Vyne
import com.orbitalhq.connectors.aws.core.registry.AwsInMemoryConnectionRegistry
import com.orbitalhq.connectors.config.aws.AwsConnectionConfiguration
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedValue
import com.orbitalhq.query.QueryResult
import com.orbitalhq.query.VyneQlGrammar
import com.orbitalhq.schema.api.SimpleSchemaProvider
import com.orbitalhq.schemas.taxi.TaxiSchema
import com.orbitalhq.testVyne
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CreateBucketRequest
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

@Testcontainers
class S3InvokerTest {
   private val bucket = "testbucket"
   private val objectKey = "myfile"
   private val localStackImage = DockerImageName.parse("localstack/localstack").withTag("1.0.4")

   @JvmField
   @Rule
   var localstack: LocalStackContainer = LocalStackContainer(localStackImage)
      .withServices(LocalStackContainer.Service.S3)
   private val defaultSchema = """
         import com.orbitalhq.aws.s3.S3Service
         import com.orbitalhq.aws.s3.S3Operation
         import  ${VyneQlGrammar.QUERY_TYPE_NAME}
         type alias Price as Decimal
         type alias Symbol as String
          @com.orbitalhq.formats.Csv(
                     delimiter = ",",
                     nullValue = "NULL"
                  )
         type OrderWindowSummary {
             symbol : Symbol by column(2)
             open : Price by column(3)
             // Added column
             high : Price by column(4)
             // Changed column
             close : Price by column(6)
         }
          @S3Service( connectionName = "vyneAws" )
          service AwsBucketService {
                 @S3Operation(bucket = "$bucket")
                 vyneQl query fetchReports(body:VyneQlQuery): OrderWindowSummary[] with capabilities {
                  filter(==,in,like)
               }
             }
""".trimIndent()

   private val connectionRegistry = AwsInMemoryConnectionRegistry()

   @Before
   fun before() {
      // Upload test CSV into S3
      val s3: S3Client = S3Client
         .builder()
         .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.S3))
         .region(Region.of(localstack.region))
         .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("not-used", "not-used")))
         .build()

      val resource = Resources.getResource("Coinbase_BTCUSD_3rows.csv").path
      s3.createBucket { b: CreateBucketRequest.Builder -> b.bucket(bucket) }
      s3.putObject({ builder -> builder.bucket(bucket).key(objectKey) }, Paths.get(resource))
      s3.putObject({ builder -> builder.bucket(bucket).key("${objectKey}2") }, Paths.get(resource))
      val connectionConfig = AwsConnectionConfiguration(
         connectionName = "vyneAws",
          region = localstack.region,
          accessKey = localstack.accessKey,
          secretKey = localstack.secretKey,
          endPointOverride = localstack.getEndpointOverride(
               LocalStackContainer.Service.S3
            ).toString()
         )
      connectionRegistry.register(connectionConfig)
   }


   @Test
   fun `can consume a csv file in s3`() {
      val resultsFromQuery = mutableListOf<TypedInstance>()
      val vyne = vyneWithS3Invoker()
      val query = runBlocking { vyne.query("""find { OrderWindowSummary[] }""") }
      collectQueryResults(query, resultsFromQuery)
      Awaitility.await().atMost(10, TimeUnit.SECONDS).until<Boolean> { resultsFromQuery.size == 6 }
      val firstObject = (resultsFromQuery.first().value as Map<String, TypedValue>)
      firstObject["symbol"]!!.value.toString().should.equal("BTCUSD")

   }

   private fun collectQueryResults(query: QueryResult, resultsFromQuery1: MutableList<TypedInstance>) {
      GlobalScope.async {
         query.results
            .collect {
               resultsFromQuery1.add(it)
            }
      }
   }


   private fun vyneWithS3Invoker(taxi: String = defaultSchema): Vyne {
      val schema = TaxiSchema.fromStrings(
         listOf(
            S3ConnectorTaxi.schema,
            VyneQlGrammar.QUERY_TYPE_TAXI,
            taxi
         )
      )
      val s3invoker = S3Invoker(connectionRegistry, SimpleSchemaProvider(schema))
      val invokers = listOf(
         s3invoker
      )
      return testVyne(schema, invokers)
   }
}
