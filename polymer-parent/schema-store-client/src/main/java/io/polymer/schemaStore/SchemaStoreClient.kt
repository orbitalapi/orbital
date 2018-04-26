package io.polymer.schemaStore

import com.diffplug.common.base.Either
import lang.taxi.CompilationError
import reactor.core.publisher.Mono

interface SchemaStoreClient {
   fun submitSchema(schemaName: String,
                    schemaVersion: String,
                    schema: String):Mono<Either<CompilationError, SchemaSetId>>

   fun schemaSet():SchemaSet
}
