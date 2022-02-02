package io.vyne.schemaServer.openapi

import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.okForContentType
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.github.tomakehurst.wiremock.junit.WireMockClassRule
import com.jayway.awaitility.Awaitility.await
import com.nhaarman.mockito_kotlin.argThat
import com.nhaarman.mockito_kotlin.atLeastOnce
import com.nhaarman.mockito_kotlin.verify
import io.vyne.schemaPublisherApi.SchemaPublisher
import io.vyne.schemaServer.SchemaPublicationConfig
import io.vyne.schemaServerCore.InMemorySchemaRepositoryConfigLoader
import io.vyne.schemaServerCore.SchemaRepositoryConfig
import io.vyne.schemaServerCore.SchemaRepositoryConfigLoader
import io.vyne.schemaServerCore.openApi.OpenApiConfiguration
import io.vyne.schemaServerCore.openApi.OpenApiSchemaRepositoryConfig
import io.vyne.schemaServerCore.openApi.OpenApiWatcher
import io.vyne.utils.withoutWhitespace
import mu.KotlinLogging
import org.intellij.lang.annotations.Language
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

@ActiveProfiles("test")
@SpringBootTest(
   webEnvironment = NONE,
   properties = [
      "vyne.schema-server.open-api.pollFrequency=PT1S"
   ]
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
               .willReturn(
                  okForContentType(
                     "application/x-yaml",
                     initialOpenApi
                  )
               )
         )
      }
   }

   @MockBean
   private lateinit var schemaPublisherMock: SchemaPublisher

   @Autowired
   lateinit var openApiWatcher: OpenApiWatcher

   @Test
   fun `when an openapi server is configured watches it for changes`() {
      openApiWatcher.pollForUpdates()
      // expect initial state to be sent with default version
      await().until {
         verify(schemaPublisherMock, atLeastOnce()).submitSchemas(
            argThat {
               val versionedSource = single()
               versionedSource.name == "petstore" &&
                  versionedSource.version == "0.1.0" &&
                  versionedSource.content.withoutWhitespace() == """
               namespace vyne.openApi {
                  type Name inherits String
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
            .willReturn(
               okForContentType(
                  "application/x-yaml",
                  updatedOpenApi
               )
            )
      )
      KotlinLogging.logger {}.info { "Updated remote service" }

      // then updated state is sent with same version
      openApiWatcher.pollForUpdates()

      verify(schemaPublisherMock, atLeastOnce()).submitSchemas(
         argThat {
            val versionedSource = single()
            versionedSource.name == "petstore" &&
               versionedSource.version == "0.1.0" &&
               versionedSource.content.withoutWhitespace() == """
               namespace vyne.openApi {
                  type FirstName inherits String
               }
               """.withoutWhitespace()
         }
      )
   }

   @Profile("test")
   @Configuration
   @Import(SchemaPublicationConfig::class, OpenApiConfiguration::class, OpenApiWatcher::class)
   class TestConfig {
      @Bean
      fun configLoader(): SchemaRepositoryConfigLoader {
         return InMemorySchemaRepositoryConfigLoader(
            SchemaRepositoryConfig(
               openApi = OpenApiSchemaRepositoryConfig(
                  services = listOf(
                     OpenApiSchemaRepositoryConfig.OpenApiServiceConfig(
                        "petstore",
                        uri = "${wireMockRule.baseUrl()}/openapi",
                        defaultNamespace = "vyne.openApi"
                     )
                  )
               )
            )
         )
      }
   }
}
