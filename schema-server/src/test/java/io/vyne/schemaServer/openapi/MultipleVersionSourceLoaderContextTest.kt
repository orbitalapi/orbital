package io.vyne.schemaServer.openapi

import arrow.core.Either
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.okForContentType
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.github.tomakehurst.wiremock.junit.WireMockClassRule
import com.winterbe.expekt.should
import io.vyne.SchemaId
import io.vyne.VersionedSource
import io.vyne.schemaServer.CompilerService
import io.vyne.schemaServer.SchemaServerApp
import io.vyne.schemaServer.file.FileChangeDetectionMethod
import io.vyne.schemaServer.git.GitSchemaConfig
import io.vyne.schemaServer.publisher.SourceWatchingSchemaPublisher
import io.vyne.schemaStore.SchemaPublisher
import io.vyne.schemas.Schema
import io.vyne.schemas.SimpleSchema
import io.vyne.utils.withoutWhitespace
import lang.taxi.CompilationException
import mu.KotlinLogging
import org.intellij.lang.annotations.Language
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import java.nio.file.Files
import java.util.concurrent.ConcurrentHashMap

@SpringBootTest(
   webEnvironment = NONE,
   properties = [
      "vyne.schema-server.open-api.pollFrequency=PT1S"
   ]
)
@ContextConfiguration(
   initializers = [MultipleVersionSourceLoaderContextTest.TestConfig::class]
)
@RunWith(SpringJUnit4ClassRunner::class)
@DirtiesContext
class MultipleVersionSourceLoaderContextTest {

   companion object {

      @ClassRule
      @JvmField
      val wireMockRule: WireMockClassRule = WireMockClassRule(options().dynamicPort())

      @ClassRule
      @JvmField
      val folder = TemporaryFolder()

      @BeforeClass
      @JvmStatic
      fun prepare() {
         val createdFile = Files.createFile(folder.root.toPath().resolve("hello.taxi"))
         createdFile.toFile().writeText("Original")

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
         wireMockRule.stubFor(
            get(urlPathEqualTo("/openapi"))
               .willReturn(
                  okForContentType(
                     "application/x-yaml",
                     initialOpenApi
                  )
               )
         )
      }
   }

   @Autowired
   lateinit var sourceWatchingSchemaPublisher: SourceWatchingSchemaPublisher

   @Autowired
   lateinit var openApiWatcher: OpenApiWatcher

   @Autowired
   lateinit var schemaPublisherStub: SchemaPublisherStub

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
      wireMockRule.stubFor(
         get(urlPathEqualTo("/openapi"))
            .willReturn(
               okForContentType(
                  "application/x-yaml",
                  updatedOpenApi
               )
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

   @Configuration
   @EnableConfigurationProperties(value = [GitSchemaConfig::class, OpenApiServicesConfig::class])
   @ComponentScan(
      basePackageClasses = [CompilerService::class],
      excludeFilters = [ComponentScan.Filter(
         type = ASSIGNABLE_TYPE,
         classes = [SchemaServerApp::class]
      )]
   )
   class TestConfig : ApplicationContextInitializer<ConfigurableApplicationContext> {
      override fun initialize(applicationContext: ConfigurableApplicationContext) {
         TestPropertyValues.of(
            "vyne.schema-server.compileOnStartup=false",
            "vyne.schema-server.file.changeDetectionMethod=${FileChangeDetectionMethod.WATCH}",
            "vyne.schema-server.file.incrementVersionOnChange=false",
            "vyne.schema-server.file.paths[0]=" + folder.root.canonicalPath,
            "vyne.schema-server.open-api.services[0].name=petstore",
            "vyne.schema-server.open-api.services[0].uri=${wireMockRule.baseUrl()}/openapi",
            "vyne.schema-server.open-api.services[0].default-namespace=vyne.openApi",
         ).applyTo(applicationContext)
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
