package io.vyne.queryService.schemas.editor

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.given
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.willAnswer
import com.winterbe.expekt.should
import io.kotest.matchers.shouldBe
import io.vyne.PackageMetadata
import io.vyne.VersionedSource
import io.vyne.queryService.schemas.BuiltInTypesProvider
import io.vyne.queryService.schemas.SubmitEditJson
import io.vyne.schema.publisher.PublisherHealth
import io.vyne.schema.publisher.PublisherType
import io.vyne.schemaServer.editor.SchemaEditRequest
import io.vyne.schemaServer.editor.SchemaEditResponse
import io.vyne.schemaServer.editor.SchemaEditorApi
import io.vyne.schemaServer.packages.PackagesServiceApi
import io.vyne.schemaServer.packages.SourcePackageDescription
import io.vyne.schemaStore.LocalValidatingSchemaStoreClient
import io.vyne.spring.http.BadRequestException
import io.vyne.utils.withoutWhitespace
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.assertThrows
import reactor.core.publisher.Mono

class LocalSchemaEditingServiceTest {
   private lateinit var editorService: LocalSchemaEditingService

   lateinit var schemaStore: LocalValidatingSchemaStoreClient

   private val schemaEditorApi = mock<SchemaEditorApi> { }
   private val packagesServiceApi = mock<PackagesServiceApi> { }
   private val objectMapper = jacksonObjectMapper()

   @Before
   fun setup() {
      schemaStore = LocalValidatingSchemaStoreClient()
      schemaStore.submitSchemas(
         BuiltInTypesProvider.versionedSources.packageMetadata,
         BuiltInTypesProvider.versionedSources.sources
      )
      editorService = LocalSchemaEditingService(packagesServiceApi, schemaEditorApi, schemaStore)
   }

   @Test
   fun `If a type has definitions across multiple packages, we should reject`() {
      setListPackagesResponse(PackageMetadata.from("io.vyne", "movies", "0.1.0"))
      val badRequestException = assertThrows<BadRequestException> {
         schemaStore.submitSchemas(
            PackageMetadata.from("io.vyne", "test", "1.0.0"), listOf(
               VersionedSource(
                  "StreamingProvider",
                  "0.1.0",
                  """namespace io.vyne.demos.film {
         |   type StreamingProvider inherits String
         |}""".trimMargin()
               )
            )
         )
         val editedSchema: EditedSchema = objectMapper.readValue(SubmitEditJson.JSON, EditedSchema::class.java)
         editorService.submitEditedSchema(editedSchema, "io.vyne/movies/0.1.0").block()
      }
      badRequestException.message.should.equal("Editing types with definitions in multiple packages is not supported. [StreamingProvider is defined in io.vyne/test/1.0.0]")
   }

   @Test
   fun `If a service has definitions across multiple packages, we should reject`() {
      setListPackagesResponse(PackageMetadata.from("io.vyne", "movies", "0.1.0"))
      val badRequestException = assertThrows<BadRequestException> {
         schemaStore.submitSchemas(
            PackageMetadata.from("io.vyne", "test", "1.0.0"), listOf(
               VersionedSource(
                  "KafkaTopicService",
                  "0.1.0",
                  """namespace io.vyne.demos.film {
         |   service KafkaTopicService { operation foo(): lang.taxi.String }
         |}""".trimMargin()
               )
            )
         )
         val editedSchema: EditedSchema = objectMapper.readValue(SubmitEditJson.JSON, EditedSchema::class.java)
         editorService.submitEditedSchema(editedSchema, "io.vyne/movies/0.1.0").block()
      }
      badRequestException.message.should.equal("Editing services with definitions in multiple packages is not supported. [KafkaTopicService is defined in io.vyne/test/1.0.0]")
   }

   @Test
   fun `changes need to be part of an editable package`() {
      setListPackagesResponse(PackageMetadata.from("io.vyne", "movies", "0.1.0"), false)
      val badRequestException = assertThrows<BadRequestException> {
         schemaStore.submitSchemas(
            PackageMetadata.from("io.vyne", "test", "1.0.0"), listOf(
               VersionedSource(
                  "StreamingProvider",
                  "0.1.0",
                  """namespace io.vyne.demos.film {
         |   type StreamingProvider inherits String
         |}""".trimMargin()
               )
            )
         )
         val editedSchema: EditedSchema = objectMapper.readValue(SubmitEditJson.JSON, EditedSchema::class.java)
         editorService.submitEditedSchema(editedSchema, "io.vyne/movies/0.1.0").block()
      }
      badRequestException.message.should.equal("io.vyne/movies/0.1.0 is not editable")
   }

   @Test
   fun `an edit can contain an insert`() {
      var expectedRequest: SchemaEditRequest? = null
      setListPackagesResponse(PackageMetadata.from("io.vyne", "movies", "0.1.0"))
      given(schemaEditorApi.submitEdits(any())).willAnswer { invocationOnMock ->
         (invocationOnMock.arguments.first() as SchemaEditRequest).also { expectedRequest = it }
         Mono.just(SchemaEditResponse(true, emptyList()))
      }
      val editedSchema: EditedSchema = objectMapper.readValue(SubmitEditJson.JSON, EditedSchema::class.java)
      editorService.submitEditedSchema(editedSchema, "io.vyne/movies/0.1.0").block()
      expectedRequest!!.edits.size.shouldBe(7)
   }

   @Test
   fun `an edit can contain an update`() {
      var expectedRequest: SchemaEditRequest? = null
      setListPackagesResponse(PackageMetadata.from("io.vyne", "movies", "0.1.0"))
      val initialSourceForStreamingProvider = """
         namespace io.vyne.demos.film {
            model StreamingProvider {
                  name : String?
                  pricePerMonth : Decimal?
                  imdbId: String?
               }
         }
      """.trimIndent()

      schemaStore.submitSchemas(
         PackageMetadata.from("io.vyne", "movies", "0.1.0"), listOf(
            VersionedSource(
               "StreamingProvider",
               "0.1.0",
               initialSourceForStreamingProvider
            )
         )
      )

      given(schemaEditorApi.submitEdits(any())).willAnswer { invocationOnMock ->
         (invocationOnMock.arguments.first() as SchemaEditRequest).also { expectedRequest = it }
         Mono.just(SchemaEditResponse(true, emptyList()))
      }
      val editedSchema: EditedSchema = objectMapper.readValue(SubmitEditJson.JSON, EditedSchema::class.java)
      editorService.submitEditedSchema(editedSchema, "io.vyne/movies/0.1.0").block()
      expectedRequest!!.edits.size.shouldBe(7)
      expectedRequest!!.edits.first { it.name == "io/vyne/demos/film/StreamingProvider.taxi" }.content
         .trimIndent().trimMargin().withoutWhitespace().trim().should.equal("""
            namespace io.vyne.demos.film {
               model StreamingProvider {
                     name : String?
                     pricePerMonth : Decimal?
                  }
            }
         """.trimIndent().trimMargin().withoutWhitespace().trim())
   }

   private fun setListPackagesResponse(packageMetadata: PackageMetadata, isEditable: Boolean = true) {
      given(packagesServiceApi.listPackages()) willAnswer {
         Mono.just(
            listOf(
               editableSourcePackageDescriptionOf(
                  packageMetadata
               ).copy(editable = isEditable)
            )
         )
      }
   }

   private fun editableSourcePackageDescriptionOf(packageMetadata: PackageMetadata): SourcePackageDescription {
      return SourcePackageDescription(
         packageMetadata.identifier,
         PublisherHealth(PublisherHealth.Status.Healthy),
         1,
         0,
         0,
         PublisherType.FileSystem,
         true
      )
   }
}
