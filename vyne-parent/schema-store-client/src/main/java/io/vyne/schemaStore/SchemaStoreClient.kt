package io.vyne.schemaStore

import io.vyne.schemas.Schema
import lang.taxi.CompilationException
import org.funktionale.either.Either
import reactor.core.publisher.Mono

interface SchemaStoreClient {
   fun submitSchema(schemaName: String,
                    schemaVersion: String,
                    schema: String):Mono<Either<CompilationException, Schema>>
   fun submitSchema(versionedSchema: VersionedSchema) = submitSchema(versionedSchema.name, versionedSchema.version, versionedSchema.content)
   fun schemaSet(): SchemaSet
}
