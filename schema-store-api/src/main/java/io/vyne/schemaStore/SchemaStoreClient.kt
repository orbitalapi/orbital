package io.vyne.schemaStore

import arrow.core.Either
import io.vyne.ParsedSource
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
   fun submitSchema(
      schemaName: String,
      schemaVersion: String,
      schema: String
   ) = submitSchema(VersionedSource(schemaName, schemaVersion, schema))

   fun submitSchema(versionedSource: VersionedSource) = submitSchemas(listOf(versionedSource))
   fun submitSchemas(versionedSources: List<VersionedSource>): Either<CompilationException, Schema>

   /**
    * Checks the schemas are valid, given the existing federated schema.
    * Does not modify the actual schema
    */
   fun validateSchemas(versionedSources: List<VersionedSource>): Either<Pair<CompilationException, List<ParsedSource>>, Pair<Schema, List<ParsedSource>>>
}




