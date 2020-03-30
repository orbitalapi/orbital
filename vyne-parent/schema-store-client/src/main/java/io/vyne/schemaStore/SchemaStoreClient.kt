package io.vyne.schemaStore

import io.vyne.VersionedSource
import io.vyne.schemas.Schema
import lang.taxi.CompilationException
import org.funktionale.either.Either
import reactor.core.publisher.Mono

interface SchemaStoreClient {
   fun submitSchema(schemaName: String,
                    schemaVersion: String,
                    schema: String) = submitSchema(VersionedSource(schemaName, schemaVersion, schema))

   fun submitSchema(versionedSchema: VersionedSource) = submitSchemas(listOf(versionedSchema))
   fun submitSchemas(schemas: List<VersionedSource>): Either<CompilationException, Schema>
   fun schemaSet(): SchemaSet

   val generation: Int
}



