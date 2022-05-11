package io.vyne.connectors.aws.lambda

import com.google.common.io.Resources
import com.winterbe.expekt.should
import io.vyne.StubService
import io.vyne.connectors.aws.core.AwsConnection
import io.vyne.connectors.aws.core.AwsConnectionConfiguration
import io.vyne.connectors.aws.core.registry.AwsInMemoryConnectionRegistry
import io.vyne.models.Provided
import io.vyne.models.TypedCollection
import io.vyne.models.TypedInstance
import io.vyne.query.VyneQlGrammar
import io.vyne.schema.api.SimpleSchemaProvider
import io.vyne.testVyne
import io.vyne.typedObjects
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import org.testcontainers.utility.MountableFile
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.math.BigDecimal
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private val logger = KotlinLogging.logger {  }
@Testcontainers
class LambdaInvokerTest {
   companion object {
      private fun File.bufferedOutputStream(size: Int = 8192) = BufferedOutputStream(this.outputStream(), size)
      private fun File.zipOutputStream(size: Int = 8192) = ZipOutputStream(this.bufferedOutputStream(size))
      private fun File.bufferedInputStream(size: Int = 8192) = BufferedInputStream(this.inputStream(), size)
      private fun File.asZipEntry() = ZipEntry(this.name)
      private val localStackImage = DockerImageName.parse("localstack/localstack").withTag("0.14.0")
      private val folder = TemporaryFolder()

      @Container
      var localStack: LocalStackContainer = LocalStackContainer(localStackImage)
         .withServices(LocalStackContainer.Service.LAMBDA)

      private fun createLambdaPackages(): String {
         folder.create()
         val filesToBeZipped = listOf(File(Resources.getResource("index.js").path))
         val packageFile = folder.newFile("index.zip")
         packageFile.zipOutputStream().use {
            filesToBeZipped.forEach { file ->
               it.putNextEntry(file.asZipEntry())
               file.bufferedInputStream().use { bis -> bis.copyTo(it) }
            }
         }
         return packageFile.path
      }

      @BeforeClass
      @JvmStatic
      fun before() {

         localStack
            .withCopyFileToContainer(MountableFile.forHostPath(createLambdaPackages()),
               "/tmp/localstack/index.zip")

         localStack.start()

         val lambdaCreationResult =  localStack.execInContainer(
            "awslocal", "lambda", "create-function",
            "--function-name", "streamingprovider",
            "--runtime", "nodejs12.x",
            "--region","us-east-1",
            "--handler", "index.handler",
            "--role", "arn:aws:iam::123456:role/test",
            "--zip-file","fileb:///tmp/localstack/index.zip",
            "--environment", "Variables={AWS_ACCESS_KEY_ID=${localStack.accessKey},AWS_SECRET_ACCESS_KEY=${localStack.secretKey}}"
         )
         logger.info { lambdaCreationResult.stdout }
         if (lambdaCreationResult.stderr.isNotBlank()) {
            logger.error { "error in creating lambda function  ${lambdaCreationResult.stderr}" }
         }

      }

   }

   private val connectionRegistry = AwsInMemoryConnectionRegistry()

   @Before
   fun beforeTest() {
      val connectionConfig = AwsConnectionConfiguration(connectionName = "vyneAws",
         mapOf(AwsConnection.Parameters.ACCESS_KEY.templateParamName to localStack.accessKey,
            AwsConnection.Parameters.SECRET_KEY.templateParamName to localStack.secretKey,
            AwsConnection.Parameters.AWS_REGION.templateParamName to localStack.region,
            AwsConnection.Parameters.ENDPOINT_OVERRIDE.templateParamName to localStack.getEndpointOverride(LocalStackContainer.Service.LAMBDA).toString()
         ))
      connectionRegistry.register(connectionConfig)
   }

   @Test
   fun `Vyne can invoke a lambda function`(): Unit = runBlocking {
      val vyne = testVyne(
         listOf(
            LambdaConnectorTaxi.schema,
            VyneQlGrammar.QUERY_TYPE_TAXI,
            """
         ${LambdaConnectorTaxi.Annotations.imports}
         import ${VyneQlGrammar.QUERY_TYPE_NAME}
         type MovieId inherits Int
         type MovieTitle inherits String
         type StreamingProviderName inherits String
         type MonthlyPrice inherits Decimal
         model Movie {
            id : MovieId
            title : MovieTitle
         }

         model StreamingProvider {
            name: StreamingProviderName
            pricePerMonth: MonthlyPrice
         }

         parameter model StreamingProviderRequest {
            filmId: MovieId
         }

         model StreamingProviderResponse {
            statusCode: Int
            body: StreamingProvider
         }

         service MovieService {
            operation listMovies(): Movie[]
         }

         @AwsLambdaService( connectionName = "vyneAws" )
         service MovieDb {
            @LambdaOperation(name = "streamingprovider")
            operation movieQuery(@RequestBody StreamingProviderRequest): StreamingProviderResponse
         }
      """
         )
      ) { schema ->
         val stubService = StubService(schema = schema)
         val stalkerMovieJson = """{
         |"id" : "1",
         |"title" : "Stalker"
         |}
      """.trimMargin()
         val solarisMovieJson = """{
         |"id" : "2",
         |"title" : "Solaris"
         |}
      """.trimMargin()
         val stalker = TypedInstance.from(schema.type("Movie"), stalkerMovieJson, schema, source = Provided)
         val solaris = TypedInstance.from(schema.type("Movie"), solarisMovieJson, schema, source = Provided)
         stubService.addResponse("listMovies", TypedCollection.from(listOf(stalker, solaris)))
         listOf(LambdaInvoker(connectionRegistry, SimpleSchemaProvider(schema)), stubService)
      }

      val result = vyne.query("""findAll { Movie[] }
            as { id : MovieId
                 pricePerMonth : MonthlyPrice
                 name: StreamingProviderName
          }[] """)
         .typedObjects()
      result.should.have.size(2)
      val sortedByFilmId = result.sortedBy { typedObject ->
         val resultAsMap = typedObject.toRawObject() as Map<String, Any?>
         resultAsMap["id"] as Int
      }.map { it.toRawObject() }
      sortedByFilmId.should.equal(listOf(
         mapOf("id" to 1, "pricePerMonth" to BigDecimal("9.99"), "name" to "Netflix"),
         mapOf("id" to 2, "pricePerMonth" to BigDecimal("7.99"), "name" to "Disney Plus")
      ))
   }




}
