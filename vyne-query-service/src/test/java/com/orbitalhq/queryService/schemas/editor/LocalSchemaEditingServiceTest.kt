package com.orbitalhq.queryService.schemas.editor

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.nhaarman.mockito_kotlin.*
import com.winterbe.expekt.should
import io.kotest.matchers.shouldBe
import com.orbitalhq.*
import com.orbitalhq.cockpit.core.schemas.BuiltInTypesProvider
import com.orbitalhq.cockpit.core.schemas.editor.*
import com.orbitalhq.cockpit.core.schemas.editor.operations.ChangeFieldType
import com.orbitalhq.cockpit.core.schemas.editor.operations.ChangeOperationParameterType
import com.orbitalhq.cockpit.core.schemas.editor.operations.CreateOrReplaceSource
import com.orbitalhq.cockpit.core.schemas.editor.operations.SchemaEdit
import com.orbitalhq.queryService.schemas.SubmitEditJson
import com.orbitalhq.schema.publisher.PublisherHealth
import com.orbitalhq.schema.publisher.PublisherType
import com.orbitalhq.schemaServer.editor.SchemaEditRequest
import com.orbitalhq.schemaServer.editor.SchemaEditResponse
import com.orbitalhq.schemaServer.editor.SchemaEditorApi
import com.orbitalhq.schemaServer.packages.PackageWithDescription
import com.orbitalhq.schemaServer.packages.PackagesServiceApi
import com.orbitalhq.schemaServer.packages.SourcePackageDescription
import com.orbitalhq.schemaStore.LocalValidatingSchemaStoreClient
import com.orbitalhq.schemas.OperationNames
import com.orbitalhq.schemas.fqn
import com.orbitalhq.spring.http.BadRequestException
import com.orbitalhq.utils.withoutWhitespace
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
      schemaStore.submitPackage(
         SourcePackage(
            BuiltInTypesProvider.sourcePackage.packageMetadata,
            BuiltInTypesProvider.sourcePackage.sources
         )
      )
      editorService = LocalSchemaEditingService(packagesServiceApi, schemaEditorApi, schemaStore)
   }

   @Test
   fun `can change the field of a type to an existing field in the same schema`() {
      whenever(packagesServiceApi.loadPackage(any())).thenReturn(
         Mono.just(
            sourcePackageOf("").withDescription()
         )
      )
      val result = editorService.submitSchemaEditOperation(
         SchemaEdit(
            PackageIdentifier.fromId("com.foo/test/1.0.0"),
            edits = listOf(
               CreateOrReplaceSource(
                  listOf(
                     VersionedSource(
                        "src.taxi", "1.0.0",
                        """
            namespace com.films {

               type FilmTitle inherits String
               model Film {
                  title : String
               }
            }
         """
                     )
                  )
               ),
               ChangeFieldType("com.films.Film".fqn(), "title", "com.films.FilmTitle".fqn())
            )
         )
      ).block()!!

      result.sourcePackage.shouldHaveSourceEqualTo(
         """
      namespace com.films {

         type FilmTitle inherits String
         model Film {
            title : com.films.FilmTitle
         }
      }
      """.trimIndent()
      )

   }


   private fun sourcePackageOf(
      source: String,
      packageIdentifier: PackageIdentifier = PackageIdentifier.fromId("test/test/1.0.0")
   ): SourcePackage {
      return SourcePackage(
         PackageMetadata.from(packageIdentifier),
         listOf(
            VersionedSource(
               "TestSrc", "1.0.0", source
            )
         )
      )
   }

   @Test
   fun `can change the field of a type to a new type`() {
      whenever(packagesServiceApi.loadPackage(any())).thenReturn(
         Mono.just(
            sourcePackageOf(
               """
            namespace com.films {

               model Film {
                  title : String
               }
            }
         """
            ).withDescription()
         )
      )
      val result = editorService.submitSchemaEditOperation(
         SchemaEdit(
            PackageIdentifier.fromId("test/test/1.0.0"),

//            ),
            edits = listOf(
               CreateOrReplaceSource(
                  listOf(
                     VersionedSource(
                        "FilmTitle", "1.0.0", """
                  namespace com.films {
                     type FilmTitle inherits String
                  }
               """
                     )
                  )
               ),
               ChangeFieldType("com.films.Film".fqn(), "title", "com.films.FilmTitle".fqn())
            )
         )
      ).block()!!

      result.sourcePackage.shouldHaveSourceEqualTo(
         """
      namespace com.films {
         model Film {
            title : com.films.FilmTitle
         }
      }

      namespace com.films {
         type FilmTitle inherits String
      }
      """.trimIndent()
      )

   }

   @Test
   fun `can change the field of a type to an existing field in another schema`() {

   }

   @Test
   fun `can change the type of an operation parameter`() {
      whenever(packagesServiceApi.loadPackage(any())).thenReturn(
         Mono.just(
            sourcePackageOf(
               """
            namespace com.films {

               model Film {
                  title : String
                  filmId: FilmId inherits Int
               }
               service FilmsService {
                  operation findById(id:String):Film
               }
            }
         """
            ).withDescription(),
         )
      )
      val result = editorService.submitSchemaEditOperation(
         SchemaEdit(
            PackageIdentifier.fromId("test/test/1.0.0"),
            listOf(
               ChangeOperationParameterType(
                  OperationNames.qualifiedName("com.films.FilmsService", "findById"),
                  "id",
                  "com.films.FilmId".fqn()
               )
            )
         )
      ).block()!!

      result.sourcePackage.shouldHaveSourceEqualTo(
         """
            namespace com.films {

               model Film {
                  title : String
                  filmId: FilmId inherits Int
               }
               service FilmsService {
                  operation findById(id:com.films.FilmId):Film
               }
            }
         """
      )
   }

   @Test
   fun `can rename a type`() {

   }

   @Test
   fun `can change the return type of an operation`() {

   }

//   @Test
//   fun `If a type has definitions across multiple packages, we should reject`() {
//      setListPackagesResponse(PackageMetadata.from("com.orbitalhq", "movies", "0.1.0"))
//      val badRequestException = assertThrows<BadRequestException> {
//         schemaStore.submitSchemas(
//            PackageMetadata.from("com.orbitalhq", "test", "1.0.0"), listOf(
//               VersionedSource(
//                  "StreamingProvider",
//                  "0.1.0",
//                  """namespace com.orbitalhq.demos.film {
//         |   type StreamingProvider inherits String
//         |}""".trimMargin()
//               )
//            )
//         )
//         val editedSchema: EditedSchema = objectMapper.readValue(SubmitEditJson.JSON, EditedSchema::class.java)
//         editorService.submitEditedSchema(editedSchema, "com.orbitalhq/movies/0.1.0").block()
//      }
//      badRequestException.message.should.equal("Editing types with definitions in multiple packages is not supported. [StreamingProvider is defined in com.orbitalhq/test/1.0.0]")
//   }
//
//   @Test
//   fun `If a service has definitions across multiple packages, we should reject`() {
//      setListPackagesResponse(PackageMetadata.from("com.orbitalhq", "movies", "0.1.0"))
//      val badRequestException = assertThrows<BadRequestException> {
//         schemaStore.submitSchemas(
//            PackageMetadata.from("com.orbitalhq", "test", "1.0.0"), listOf(
//               VersionedSource(
//                  "KafkaTopicService",
//                  "0.1.0",
//                  """namespace com.orbitalhq.demos.film {
//         |   service KafkaTopicService { operation foo(): lang.taxi.String }
//         |}""".trimMargin()
//               )
//            )
//         )
//         val editedSchema: EditedSchema = objectMapper.readValue(SubmitEditJson.JSON, EditedSchema::class.java)
//         editorService.submitEditedSchema(editedSchema, "com.orbitalhq/movies/0.1.0").block()
//      }
//      badRequestException.message.should.equal("Editing services with definitions in multiple packages is not supported. [KafkaTopicService is defined in com.orbitalhq/test/1.0.0]")
//   }

   @Test
   fun `changes need to be part of an editable package`() {
      whenever(packagesServiceApi.loadPackage(any())).thenReturn(
         Mono.just(
            sourcePackageOf("").withDescription(editable = false)
         )
      )
      val badRequestException = assertThrows<BadRequestException> {
         editorService.submitSchemaEditOperation(
            SchemaEdit(
               PackageIdentifier.fromId("com.orbitalhq/movies/0.1.0"),
               listOf(
                  ChangeOperationParameterType(
                     OperationNames.qualifiedName("com.films.FilmsService", "findById"),
                     "id",
                     "com.films.FilmId".fqn()
                  )
               )
            )
         ).block()!!
      }
      badRequestException.message.should.equal("com.orbitalhq/movies/0.1.0 is not editable")
   }

//   @Test
//   fun `an edit can contain an insert`() {
//      var expectedRequest: SchemaEditRequest? = null
//      setListPackagesResponse(PackageMetadata.from("com.orbitalhq", "movies", "0.1.0"))
//      given(schemaEditorApi.submitEdits(any())).willAnswer { invocationOnMock ->
//         (invocationOnMock.arguments.first() as SchemaEditRequest).also { expectedRequest = it }
//         Mono.just(SchemaEditResponse(true, emptyList()))
//      }
//      val editedSchema: EditedSchema = objectMapper.readValue(SubmitEditJson.JSON, EditedSchema::class.java)
//      editorService.submitEditedSchema(editedSchema, "com.orbitalhq/movies/0.1.0").block()
//      expectedRequest!!.edits.size.shouldBe(7)
//   }
//
//   @Test
//   fun `an edit can contain an update`() {
//      var expectedRequest: SchemaEditRequest? = null
//      setListPackagesResponse(PackageMetadata.from("com.orbitalhq", "movies", "0.1.0"))
//      val initialSourceForStreamingProvider = """
//         namespace com.orbitalhq.demos.film {
//            model StreamingProvider {
//                  name : String?
//                  pricePerMonth : Decimal?
//                  imdbId: String?
//               }
//         }
//      """.trimIndent()
//
//      schemaStore.submitSchemas(
//         PackageMetadata.from("com.orbitalhq", "movies", "0.1.0"), listOf(
//            VersionedSource(
//               "StreamingProvider",
//               "0.1.0",
//               initialSourceForStreamingProvider
//            )
//         )
//      )
//
//      given(schemaEditorApi.submitEdits(any())).willAnswer { invocationOnMock ->
//         (invocationOnMock.arguments.first() as SchemaEditRequest).also { expectedRequest = it }
//         Mono.just(SchemaEditResponse(true, emptyList()))
//      }
//      val editedSchema: EditedSchema = objectMapper.readValue(SubmitEditJson.JSON, EditedSchema::class.java)
//      editorService.submitEditedSchema(editedSchema, "com.orbitalhq/movies/0.1.0").block()
//      expectedRequest!!.edits.size.shouldBe(7)
//      expectedRequest!!.edits.first { it.name == "io/vyne/demos/film/StreamingProvider.taxi" }.content
//         .trimIndent().trimMargin().withoutWhitespace().trim().should.equal(
//            """
//            namespace com.orbitalhq.demos.film {
//               model StreamingProvider {
//                     name : String?
//                     pricePerMonth : Decimal?
//                  }
//            }
//         """.trimIndent().trimMargin().withoutWhitespace().trim()
//         )
//   }

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
         true,
         null
      )
   }
}

private fun SourcePackage.shouldHaveSourceEqualTo(expected: String) {
   val concatenatedSources = this.sources.joinToString("\n") { it.content }
   val expectedWithoutWhitespace = expected.withoutWhitespace()
   val actualWithoutWhitespace = concatenatedSources.withoutWhitespace()
   actualWithoutWhitespace.shouldBe(expectedWithoutWhitespace)
}

fun SourcePackage.withDescription(editable: Boolean = true): PackageWithDescription {
   return PackageWithDescription(
      this.parsed(),
      SourcePackageDescription(
         this.identifier,
         PublisherHealth(PublisherHealth.Status.Healthy),
         1,
         0,
         0,
         PublisherType.FileSystem,
         editable,
         null
      )
   )
}

fun SourcePackage.parsed(): ParsedPackage {
   return ParsedPackage(
      this.packageMetadata,
      this.sources.map { ParsedSource(it) },
      emptyMap()
   )
}
