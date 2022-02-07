package io.vyne.schemaServer.openapi

import arrow.core.Either
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.okForContentType
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.common.FileSource
import com.github.tomakehurst.wiremock.extension.Parameters
import com.github.tomakehurst.wiremock.extension.ResponseTransformer
import com.github.tomakehurst.wiremock.http.HttpHeader
import com.github.tomakehurst.wiremock.http.HttpHeaders
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.Response
import com.github.tomakehurst.wiremock.client.WireMock
import com.winterbe.expekt.should
import io.vyne.SchemaId
import io.vyne.VersionedSource
import io.vyne.schemaPublisherApi.SchemaPublisher
import io.vyne.schemaServer.SchemaPublicationConfig
import io.vyne.schemaServer.core.InMemorySchemaRepositoryConfigLoader
import io.vyne.schemaServer.core.SchemaRepositoryConfig
import io.vyne.schemaServer.core.SchemaRepositoryConfigLoader
import io.vyne.schemaServer.core.file.FileChangeDetectionMethod
import io.vyne.schemaServer.core.file.FileSystemMonitorLifecycleHandler
import io.vyne.schemaServer.core.file.FileSystemSchemaRepositoryConfig
import io.vyne.schemaServer.core.file.FileWatcherBuilders
import io.vyne.schemaServer.core.openApi.OpenApiConfiguration
import io.vyne.schemaServer.core.openApi.OpenApiSchemaRepositoryConfig
import io.vyne.schemaServer.core.openApi.OpenApiWatcher
import io.vyne.schemaServer.core.publisher.SourceWatchingSchemaPublisher
import io.vyne.schemas.Schema
import io.vyne.schemas.SimpleSchema
import io.vyne.utils.withoutWhitespace
import lang.taxi.CompilationException
import mu.KotlinLogging
import org.intellij.lang.annotations.Language
import org.junit.Before
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import wiremock.org.apache.http.HttpHeaders.CONNECTION
import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap

@ActiveProfiles("test")
@SpringBootTest(
   webEnvironment = NONE,
   properties = [
      "vyne.schema-server.compileOnStartup=false",
      "wiremock.server.baseUrl=http://localhost:\${wiremock.server.port}"
   ]
)
@RunWith(SpringJUnit4ClassRunner::class)
@AutoConfigureWireMock(port = 0)
@DirtiesContext
class MultipleVersionSourceLoaderContextTest {


   @Language("yaml")
   val initialOpenApi = """
            openapi: "3.0.0"
            info:
              version: 1.0.0
              title: Swagger Petstore
            components:
              schemas:
                Name:
                  type: string
            paths: {}
            """.trimIndent()


   companion object {
      @ClassRule
      @JvmField
      val folder = TemporaryFolder()

      @BeforeClass
      @JvmStatic
      fun prepare() {
         val createdFile = Files.createFile(folder.root.toPath().resolve("hello.taxi"))
         createdFile.toFile().writeText("Original")


      }
   }

   @Autowired
   lateinit var sourceWatchingSchemaPublisher: SourceWatchingSchemaPublisher

   @Autowired
   lateinit var openApiWatcher: OpenApiWatcher

   @Autowired
   lateinit var schemaPublisherStub: SchemaPublisherStub

   @Before
   fun setUp() {
      WireMock.stubFor(
         get(urlPathEqualTo("/openapi"))
            .willReturn(
               okForContentType(
                  "application/x-yaml",
                  initialOpenApi
               ).withHeader(CONNECTION, "close")
            )
      )
   }

   @Test
   fun `when an openapi server is configured with multiple sources watches it for changes`() {

      // expect initial state to be sent with default version
      sourceWatchingSchemaPublisher.refreshAllSources()
      schemaPublisherStub.sources.should.have.size(2)
      schemaPublisherStub.sources.any { versionedSource ->
         versionedSource.name == "petstore" &&
            versionedSource.version == "0.1.0" &&
            versionedSource.content.withoutWhitespace() == """
            namespace vyne.openApi {
               type Name inherits String
            }
            """.withoutWhitespace()
      }.should.be.`true`
      schemaPublisherStub.sources.should.contain(
         VersionedSource(
            name = "hello.taxi",
            version = "0.1.0",
            content = "Original"
         )
      )


      // when remote api is updated
      @Language("yaml")
      val updatedOpenApi = """
            openapi: "3.0.0"
            info:
              version: 1.0.0
              title: Swagger Petstore
            components:
              schemas:
                FirstName:
                  type: string
            paths: {}
            """.trimIndent()

      WireMock.reset()

      WireMock.stubFor(
         get(urlPathEqualTo("/openapi"))
            .willReturn(
               okForContentType(
                  "application/x-yaml",
                  updatedOpenApi
               ).withHeader(CONNECTION, "close")
            )
      )
      KotlinLogging.logger {}.info { "Updated remote service" }

      // Poll to force the openApi watcher not to use the cache
      openApiWatcher.pollForUpdates()
      // Then refresh
      sourceWatchingSchemaPublisher.refreshAllSources()


      schemaPublisherStub.sources.should.have.size(2)
      schemaPublisherStub.sources.any { versionedSource ->
         versionedSource.name == "petstore" &&
            versionedSource.version == "0.1.0" &&
            versionedSource.content.withoutWhitespace() == """
            namespace vyne.openApi {
               type FirstName inherits String
            }
            """.withoutWhitespace()
      }.should.be.`true`
      schemaPublisherStub.sources.should.contain(
         VersionedSource(
            name = "hello.taxi",
            version = "0.1.0",
            content = "Original"
         )
      )
   }

   @Profile("test")
   @Configuration
   @Import(
      SchemaPublicationConfig::class, OpenApiConfiguration::class, OpenApiWatcher::class, FileWatcherBuilders::class,
      FileSystemMonitorLifecycleHandler::class
   )
   class TestConfig {
      @Bean
      fun configLoader(@Value("\${wiremock.server.baseUrl}") wireMockServerBaseUrl: String): SchemaRepositoryConfigLoader {
         return InMemorySchemaRepositoryConfigLoader(
            SchemaRepositoryConfig(
               file = FileSystemSchemaRepositoryConfig(
                  changeDetectionMethod = FileChangeDetectionMethod.WATCH,
                  incrementVersionOnChange = false,
                  paths = listOf(folder.root.toPath())
               ),
               openApi = OpenApiSchemaRepositoryConfig(
                  services = listOf(
                     OpenApiSchemaRepositoryConfig.OpenApiServiceConfig(
                        "petstore",
                        uri = "$wireMockServerBaseUrl/openapi",
                        defaultNamespace = "vyne.openApi"
                     )
                  )
               )
            )
         )
      }

      @Bean
      fun schemaPublisher() = SchemaPublisherStub()
   }
}

class SchemaPublisherStub : SchemaPublisher {
   private val _sources = ConcurrentHashMap<String, VersionedSource>()
   val sources: List<VersionedSource>
      get() = _sources.values.toList()

   override fun submitSchemas(
      versionedSources: List<VersionedSource>,
      removedSources: List<SchemaId>
   ): Either<CompilationException, Schema> {
      _sources.putAll(versionedSources.associateBy { it.name })
      removedSources.forEach { _sources.remove(VersionedSource.nameAndVersionFromId(it).first) }
      return Either.right(SimpleSchema.EMPTY)
   }
}

class ConnectionCloseExtension : ResponseTransformer() {
   override fun getName(): String = "ConnectionCloseExtension"

   override fun transform(request: Request, response: Response, files: FileSource?, parameters: Parameters?): Response {
      return Response.Builder
         .like(response)
         .headers(HttpHeaders.copyOf(response.headers)
            .plus(HttpHeader("Connection", "Close")))
         .build()
   }

}
