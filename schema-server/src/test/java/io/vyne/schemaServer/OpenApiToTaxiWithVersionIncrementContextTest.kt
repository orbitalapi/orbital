package io.vyne.schemaServer

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.github.tomakehurst.wiremock.junit.WireMockClassRule
import com.jayway.awaitility.Awaitility.await
import com.nhaarman.mockito_kotlin.argThat
import com.nhaarman.mockito_kotlin.atLeastOnce
import com.nhaarman.mockito_kotlin.verify
import com.winterbe.expekt.should
import io.vyne.schemaServer.file.FilePoller
import io.vyne.schemaServer.file.FileWatcher
import io.vyne.schemaServer.git.GitSchemaRepoConfig
import io.vyne.schemaServer.git.GitSyncTask
import io.vyne.schemaServer.openapi.OpenApiServicesConfig
import io.vyne.schemaStore.SchemaPublisher
import io.vyne.utils.withoutWhitespace
import mu.KotlinLogging
import org.intellij.lang.annotations.Language
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

@SpringBootTest(
   webEnvironment = NONE,
   properties = [
      "taxi.openApiPollPeriodMs=1000"
   ]
)
@ContextConfiguration(
   initializers = [OpenApiToTaxiWithVersionIncrementContextTest.TestConfig::class]
)
@RunWith(SpringJUnit4ClassRunner::class)
@DirtiesContext
class OpenApiToTaxiWithVersionIncrementContextTest {

   companion object {

      @ClassRule
      @JvmField
      val wireMockRule: WireMockClassRule = WireMockClassRule(options().dynamicPort())

      @BeforeClass
      @JvmStatic
      fun prepare() {
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
               .willReturn(okForContentType(
                  "application/x-yaml",
                  initialOpenApi
               ))
         )
      }
   }

   @MockBean
   private lateinit var schemaPublisherMock: SchemaPublisher

   @Autowired
   private var fileWatcher: FileWatcher? = null

   @Autowired
   private var filePoller: FilePoller? = null

   @Autowired
   private var gitSyncTask: GitSyncTask? = null

   @Test
   fun `when an openapi server is configured watches it for changes`() {

      filePoller.should.be.`null`
      fileWatcher.should.be.`null`
      gitSyncTask.should.be.`null`

      // expect initial state to be sent with default version
      await().until {
         verify(schemaPublisherMock, atLeastOnce()).submitSchemas(
            argThat {
               val versionedSource = single()
               versionedSource.name == "petstore" &&
               versionedSource.version == "0.1.0" &&
               versionedSource.content.withoutWhitespace() == """
               namespace vyne.openApi {
                  type Name
               }
               """.withoutWhitespace()
            }
         )
      }
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
            .willReturn(okForContentType(
               "application/x-yaml",
               updatedOpenApi
            ))
      )
      KotlinLogging.logger {}.info { "Updated remote service" }

      // then updated state is sent with same version
      await().until {
         verify(schemaPublisherMock, atLeastOnce()).submitSchemas(
            argThat {
               val versionedSource = single()
               versionedSource.name == "petstore" &&
               versionedSource.version == "0.1.0" &&
               versionedSource.content.withoutWhitespace() == """
               namespace vyne.openApi {
                  type FirstName
               }
               """.withoutWhitespace()
            }
         )
      }
   }

   @Configuration
   @EnableAsync
   @EnableScheduling
   @EnableConfigurationProperties(value = [GitSchemaRepoConfig::class, OpenApiServicesConfig::class])
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
            "taxi.open-api-services[0].name=petstore",
            "taxi.open-api-services[0].uri=${wireMockRule.baseUrl()}/openapi",
            "taxi.open-api-services[0].default-namespace=vyne.openApi",
         ).applyTo(applicationContext)
      }
   }
}
