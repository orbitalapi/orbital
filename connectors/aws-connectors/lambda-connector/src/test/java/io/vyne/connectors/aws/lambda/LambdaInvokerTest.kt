package com.orbitalhq.connectors.aws.lambda

import com.google.common.io.Resources
import com.orbitalhq.StubService
import com.orbitalhq.connectors.aws.core.registry.AwsInMemoryConnectionRegistry
import com.orbitalhq.connectors.config.aws.AwsConnectionConfiguration
import com.orbitalhq.models.Provided
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.query.VyneQlGrammar
import com.orbitalhq.schema.api.SimpleSchemaProvider
import com.orbitalhq.testVyne
import com.orbitalhq.typedObjects
import com.winterbe.expekt.should
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import org.testcontainers.utility.MountableFile
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.lambda.LambdaClient
import software.amazon.awssdk.services.lambda.model.CreateFunctionRequest
import software.amazon.awssdk.services.lambda.model.FunctionCode
import software.amazon.awssdk.services.lambda.model.GetFunctionConfigurationRequest
import software.amazon.awssdk.services.lambda.model.GetFunctionRequest
import software.amazon.awssdk.services.lambda.waiters.LambdaWaiter
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.math.BigDecimal
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.jvm.optionals.getOrNull

private val logger = KotlinLogging.logger {  }
@Testcontainers
class LambdaInvokerTest {
   @JvmField
   @Rule
   var localStack: LocalStackContainer = LocalStackContainer(localStackImage)
      .withServices(LocalStackContainer.Service.LAMBDA)
      .withEnv("LAMBDA_SYNCHRONOUS_CREATE", "1")
      .withEnv("DEBUG", "1")

   companion object {
      private fun File.bufferedOutputStream(size: Int = 8192) = BufferedOutputStream(this.outputStream(), size)
      private fun File.zipOutputStream(size: Int = 8192) = ZipOutputStream(this.bufferedOutputStream(size))
      private fun File.bufferedInputStream(size: Int = 8192) = BufferedInputStream(this.inputStream(), size)
      private fun File.asZipEntry() = ZipEntry(this.name)
      private val localStackImage = DockerImageName.parse("localstack/localstack").withTag("3.0")
      private val folder = TemporaryFolder()

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
   }

   private val connectionRegistry = AwsInMemoryConnectionRegistry()

   @Before
   fun beforeTest() {
     val lambdaClient =  LambdaClient.builder()
         .endpointOverride(localStack.getEndpointOverride(LocalStackContainer.Service.LAMBDA))
         .region(Region.of(localStack.region))
         .build()

      val codeZip = File(createLambdaPackages()).inputStream()
      val codeBytes = SdkBytes.fromInputStream(codeZip)
      val functionBuildRequest =  CreateFunctionRequest.builder()
         .functionName("streamingprovider")
         .runtime(software.amazon.awssdk.services.lambda.model.Runtime.NODEJS12_X)
         .handler("index.handler")
         .code(FunctionCode.builder().zipFile(codeBytes).build())
         .role("arn:aws:iam::123456789012:role/irrelevant")
         .timeout(60)
         .memorySize(512)
         .build()

      lambdaClient.createFunction(functionBuildRequest)

      val waiterResponse = LambdaWaiter
         .builder()
         .client(lambdaClient)
         .build()
         .waitUntilFunctionActive(
         GetFunctionConfigurationRequest.builder().functionName("streamingprovider").build())

      val connectionConfig = AwsConnectionConfiguration(connectionName = "vyneAws",
          region = localStack.region,
          accessKey = localStack.accessKey,
          secretKey = localStack.secretKey,
          endPointOverride = localStack.getEndpointOverride(LocalStackContainer.Service.LAMBDA).toASCIIString()
      )
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

      val result = vyne.query(
         """find { Movie[] }
            as { id : MovieId
                 pricePerMonth : MonthlyPrice
                 name: StreamingProviderName
          }[] """
      )
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
