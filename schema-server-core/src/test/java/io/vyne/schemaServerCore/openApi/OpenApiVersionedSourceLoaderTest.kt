package io.vyne.schemaServerCore.openApi

import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.notFound
import com.github.tomakehurst.wiremock.client.WireMock.okForContentType
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import com.github.tomakehurst.wiremock.junit.WireMockRule
import com.winterbe.expekt.should
import io.vyne.VersionedSource
import io.vyne.utils.withoutEmptyLines
import org.intellij.lang.annotations.Language
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.URI
import java.time.Duration

class OpenApiVersionedSourceLoaderTest {

   @Rule
   @JvmField
   val openApiService = WireMockRule(options().dynamicPort())

   lateinit var openApiVersionedSourceLoader: OpenApiVersionedSourceLoader

   private val readTimeout: Duration = Duration.ofSeconds(1)

   @Before
   fun initialise() {
      openApiVersionedSourceLoader = OpenApiVersionedSourceLoader(
         name = "test",
         url = URI(openApiService.baseUrl() + "/openapi"),
         defaultNamespace = "vyne.openapi",
         connectTimeout = Duration.ofMillis(100),
         readTimeout = readTimeout,
      )
   }

   @Test
   fun `returns sources converted to a single versioned source`() {
      val initialOpenApi = openApiYaml("Person")
      openApiService.returnsOpenApiYaml(initialOpenApi)

      val sources = openApiVersionedSourceLoader.loadVersionedSources(false).sanitise()

      sources.should.equal(listOf(VersionedSource(
         "test",
         "0.1.0",
         """
         namespace vyne.openapi {
            model Person {
               name : String
            }
         }
         """.trimIndent()
      )))
   }

   @Test
   fun `returns updated sources converted to a single versioned source`() {

      // given
      openApiService.returnsOpenApiYaml(openApiYaml("Person"))
      openApiVersionedSourceLoader.loadVersionedSources(false).sanitise()

      // and the open api yaml has changed
      openApiService.returnsOpenApiYaml(openApiYaml("Human"))

      // when the sources are loaded again
      val sources = openApiVersionedSourceLoader.loadVersionedSources(cachedValuePermissible = false).sanitise()

      // then the updated version is returned
      sources.should.equal(listOf(VersionedSource(
         "test",
         "0.1.0",
         """
         namespace vyne.openapi {
            model Human {
               name : String
            }
         }
         """.trimIndent()
      )))
   }

   @Test
   fun `returns cached values unless explicitly instructed`() {

      // given
      openApiService.returnsOpenApiYaml(openApiYaml("Person"))
      openApiVersionedSourceLoader.loadVersionedSources(false).sanitise()

      // and the open api yaml has changed
      openApiService.returnsOpenApiYaml(openApiYaml("Human"))

      // when the sources are loaded again, but permitting cached values
      val sources = openApiVersionedSourceLoader.loadVersionedSources(cachedValuePermissible = true).sanitise()

      // then the updated version is returned
      sources.should.equal(listOf(VersionedSource(
         "test",
         "0.1.0",
         """
         namespace vyne.openapi {
            model Person {
               name : String
            }
         }
         """.trimIndent()
      )))
   }


   @Test
   fun `throws an exception if the remote call fails`() {
      openApiService.stubFor(
         get(urlPathEqualTo("/openapi"))
            .willReturn(
               notFound()
            )
      )

      assertThrows(IOException::class.java) {
         openApiVersionedSourceLoader.loadVersionedSources(false)
      }
   }

   @Test
   fun `throws an exception if the remote call takes too long`() {
      openApiService.stubFor(
         get(urlPathEqualTo("/openapi"))
            .willReturn(
               okForContentType(
                  "application/x-yaml",
                  openApiYaml("Whatever"),
               ).withFixedDelay(
                  readTimeout.plusMillis(20).toMillis().toInt()
               )
            )
      )

      assertThrows(SocketTimeoutException::class.java) {
         openApiVersionedSourceLoader.loadVersionedSources(false)
      }
   }
}

@Language("yaml")
private fun openApiYaml(schemaName: String) = """
      openapi: "3.0.0"
      info:
        version: 1.0.0
        title: Swagger Petstore
      components:
        schemas:
          $schemaName:
            properties:
              name:
                type: string
      paths: {}
      """.trimIndent()

private fun VersionedSource.sanitise() = copy(content = this.content.withoutEmptyLines())
private fun List<VersionedSource>.sanitise() = map(VersionedSource::sanitise)

private fun WireMockRule.returnsOpenApiYaml(
   initialOpenApi: String
) {
   stubFor(
      get(urlPathEqualTo("/openapi"))
         .willReturn(
            okForContentType(
               "application/x-yaml",
               initialOpenApi
            )
         )
   )
}
