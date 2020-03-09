package io.vyne.schemaStore

import io.vyne.schemas.Schema
import lang.taxi.CompilationException
import org.funktionale.either.Either
import reactor.core.publisher.Mono

interface SchemaStoreClient {
   fun submitSchema(schemaName: String,
                    schemaVersion: String,
                    schema: String) = submitSchema(VersionedSchema(schemaName, schemaVersion, schema))

   fun submitSchema(versionedSchema: VersionedSchema) = submitSchemas(listOf(versionedSchema))
   fun submitSchemas(schemas: List<VersionedSchema>): Either<CompilationException, Schema>
   fun schemaSet(): SchemaSet

   val generation: Int
}



