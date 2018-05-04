package io.polymer.schemaStore

import io.osmosis.polymer.schemas.Schema
import lang.taxi.CompilationException
import org.funktionale.either.Either
import reactor.core.publisher.Mono

interface SchemaStoreClient {
   fun submitSchema(schemaName: String,
                    schemaVersion: String,
                    schema: String):Mono<Either<CompilationException, Schema>>

   fun schemaSet():SchemaSet
}
