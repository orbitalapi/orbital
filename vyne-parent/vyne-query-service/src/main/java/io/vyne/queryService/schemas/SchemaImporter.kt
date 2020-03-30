package io.vyne.queryService.schemas

import io.vyne.VersionedSource
import io.vyne.schemaStore.SchemaStoreClient
import io.vyne.schemas.Schema
import lang.taxi.CompilationException
import lang.taxi.generators.Message
import org.apache.commons.io.IOUtils
import org.funktionale.either.Either
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.net.URL

@Component
class CompositeSchemaImporter(private val importers: List<SchemaImporter>, private val schemaStoreClient: SchemaStoreClient) : SchemaImportService {
   override fun preview(request: SchemaPreviewRequest): Mono<SchemaPreview> {
      val importer = importer(request.format)
      val preview = importer.preview(request)
      return Mono.just(preview)
   }

   override fun import(request: SchemaImportRequest): Mono<VersionedSource> {
      val importer = importer(request.format)
      val taxiSchema = importer.import(request)
      val compilerResult = schemaStoreClient.submitSchema(taxiSchema)
      return if (compilerResult.isLeft()) {
         throw compilerResult.left().get()
      } else {
         Mono.just(taxiSchema)
      }
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

interface SchemaImportService {
   fun import(request: SchemaImportRequest): Mono<VersionedSource>
   fun preview(request: SchemaPreviewRequest): Mono<SchemaPreview>
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
   val content: String
)

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

