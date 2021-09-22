package io.vyne.schemaStore

import arrow.core.Either
import io.vyne.SchemaId
import io.vyne.VersionedSource
import io.vyne.schemas.Schema
import lang.taxi.CompilationException

interface SchemaStoreClient : SchemaStore, SchemaPublisher

/**
 * Responsible for storing the retrieved schemas
 * and storing locally for usage
 */
interface SchemaStore {
   fun schemaSet(): SchemaSet
   val generation: Int
}

/**
 * Schema publisher is responsible for taking a provided
 * schema source, and publishing it to the vyne ecosystem
 */
interface SchemaPublisher {
   fun submitSchema(schemaName: String,
                    schemaVersion: String,
                    schema: String) = submitSchema(VersionedSource(schemaName, schemaVersion, schema))
   fun submitSchema(versionedSource: VersionedSource) = submitSchemas(listOf(versionedSource), emptyList())
   fun submitSchemas(versionedSources: List<VersionedSource>): Either<CompilationException, Schema> = submitSchemas(versionedSources, emptyList())
   fun submitSchemas(versionedSources: List<VersionedSource>, removedSources: List<SchemaId> = emptyList()): Either<CompilationException, Schema>

   fun validate(versionedSources: List<VersionedSource>, removedSources: List<SchemaId> = emptyList()): Either<CompilationException, Schema> {
      TODO("Validation is not implemented by this schema publisher (${this::class.simpleName})")
   }
}




