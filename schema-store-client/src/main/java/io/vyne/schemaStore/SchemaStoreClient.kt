package io.vyne.schemaStore

import arrow.core.Either
import io.vyne.VersionedSource
import io.vyne.schemas.Schema
import lang.taxi.CompilationException

interface SchemaStoreClient {
   fun submitSchema(schemaName: String,
                    schemaVersion: String,
                    schema: String) = submitSchema(VersionedSource(schemaName, schemaVersion, schema))

   fun submitSchema(versionedSource: VersionedSource) = submitSchemas(listOf(versionedSource))
   fun submitSchemas(versionedSources: List<VersionedSource>): Either<CompilationException, Schema>
   fun schemaSet(): SchemaSet

   val generation: Int
}



