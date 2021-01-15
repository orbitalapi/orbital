package io.vyne.queryService.schemas

import arrow.core.Either
import io.vyne.VersionedSource
import io.vyne.queryService.OperationNotPermittedException
import io.vyne.queryService.QueryServerConfig
import io.vyne.schemaStore.SchemaPublisher
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.fqn
import io.vyne.utils.log
import lang.taxi.generators.Message
import org.apache.commons.io.IOUtils
import org.springframework.http.HttpHeaders
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.net.URL
import java.util.*

@RestController
class SchemaEditService(
   private val importers: List<SchemaImporter>,
   private val schemaPublisher: SchemaPublisher,
   private val config: QueryServerConfig,
   private val schemaEditingService: SchemaEditingService
) {

   @PostMapping(path = ["/api/schemas/preview"])
   fun preview(@RequestBody request: SchemaPreviewRequest): Mono<SchemaPreview> {
      val importer = importer(request.format)
      val preview = importer.preview(request)
      return Mono.just(preview)
   }

   @PostMapping(path = ["/api/schemas"])
   @PreAuthorize(value = "isAuthenticated()")
   fun import(
      @RequestBody request: SchemaImportRequest,
      @RequestHeader(HttpHeaders.AUTHORIZATION) authorizationHeader: String
   ): Mono<VersionedSource> {
      if (!config.newSchemaSubmissionEnabled) {
         throw OperationNotPermittedException("Schema imports are currently disabled")
      }

      log().info("Received SchemaImportRequest ${request.id} with format ${request.format}.  Attempting to import")

      val importer = importer(request.format)
      val taxiSchema = importer.import(request)
      log().info("SchemaImportRequest ${request.id} generated ${taxiSchema.id}.  Attempting to validate.")
      val validationResult = schemaPublisher.validateSchemas(listOf(taxiSchema))

      if (validationResult is Either.Left) {
         val (compilationException, _) = validationResult.a;
         val errors = compilationException.errors.joinToString("\n")
         log().warn("SchemaImportRequest ${request.id} failed validation.  Compilation errors: \n$errors")
         throw compilationException
      }

      log().info("SchemaImportRequest ${request.id} validated successfully.  Publishing to schema store")
      TODO()
   }

   private fun importer(format: String): SchemaImporter {
      val importer = this.importers.firstOrNull { it.supportedFormats.contains(format) }
         ?: error("No imported found for format $format")
      return importer
   }
}

interface SchemaImporter {
   val supportedFormats: List<String>
   fun preview(request: SchemaPreviewRequest): SchemaPreview
   fun import(request: SchemaImportRequest): VersionedSource
}

data class DraftSchemaSpec(
   val name: String?,
   val version: String?,
   val defaultNamespace: String?
)

data class SchemaSpec(
   val name: String,
   val version: String,
   val defaultNamespace: String
)

data class SchemaPreviewRequest(
   val spec: DraftSchemaSpec,
   val format: String,
   val text: String?,
   val url: String?
) {
   val name: String? = spec.name
   val version: String? = spec.version
   val defaultNamespace: String? = spec.defaultNamespace

   val content: String by lazy {
      text ?: IOUtils.toString(URL(url))
   }
}

data class SchemaImportRequest(
   val spec: SchemaSpec,
   val format: String,
   val content: String,
   val id: String = UUID.randomUUID().toString()
)

/**
 * Schema edit requests are used when modifying an existing type within the schema.
 * Mass import (eg., importing a swagger spec) is not supported using this request.
 *
 * Currently we only support modification of type extensions.
 *
 * Considerations for supporting modification of definitions:
 *
 * Types can only be defined in a single place (unlike extensions).
 * However, a type can exist in a file that has other types.  Also, types can have things like
 * commented out code, which is ignored by the compiler.
 * So editing the type through the UI would lead to destruction of these comments.
 * That's probably OK, since the user is expected to be using a merge review process.
 *
 * Also, editing a type def requires parsing an exiting source, mutating one of the definitions,
 * and then reserializing it.  A lot of the independent parts are present for this, but not all working
 * together.
 *
 */
data class EditTypeRequest(
   val typeName: String,
   val kind: EditKind,
   val content: String
) {
   enum class EditKind {
      /**
       * A creation or modification to a type extension
       */
      EXTENSION,

      /**
       * A change to the actual type definition.
       */
      DEFINITION
   }

   val typeQualifiedName: QualifiedName = typeName.fqn()
}

/**
 * shows the result of a parsed schema, which hasn't yet been imported.
 * works as a preview to indicate what the resulting schema will look like.
 * It's possible that many of the attributes are null, if the parser
 * is unable to infer them, or they weren't provided
 */
data class SchemaPreview(
   val spec: DraftSchemaSpec,
   val content: String,
   val messages: List<Message>
)

