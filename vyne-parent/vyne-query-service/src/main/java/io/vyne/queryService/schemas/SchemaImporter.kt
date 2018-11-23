package io.vyne.queryService.schemas

import io.vyne.schemaStore.SchemaStoreClient
import io.vyne.schemaStore.VersionedSchema
import io.vyne.schemas.Schema
import lang.taxi.CompilationException
import org.funktionale.either.Either
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class CompositeSchemaImporter(private val importers: List<SchemaImporter>, private val schemaStoreClient: SchemaStoreClient) : SchemaImportService {
   override fun import(name: String, version: String, content: String, format: String): Mono<VersionedSchema> {
      val importer = this.importers.firstOrNull { it.supportedFormats.contains(format) }
         ?: error("No imported found for format $format")
      val taxiSchema = importer.import(name, version, content)
      return schemaStoreClient.submitSchema(taxiSchema)
         .map { compilerResult: Either<CompilationException, Schema> ->
            if (compilerResult.isLeft()) {
               throw compilerResult.left().get()
            } else {
               taxiSchema
            }
         }
   }
}

interface SchemaImporter {
   val supportedFormats: List<String>
   fun import(name: String, version: String, content: String): VersionedSchema
}

interface SchemaImportService {
   fun import(name: String, version: String, content: String, format: String): Mono<VersionedSchema>
}
