package io.vyne.schemaApi

import arrow.core.Either
import io.vyne.ParsedSource
import io.vyne.SchemaId
import io.vyne.VersionedSource
import io.vyne.schemas.Schema
import lang.taxi.CompilationError


interface SchemaValidator {
   fun validate(existing: SchemaSet, newSchema: VersionedSource) = validate(existing, listOf(newSchema), emptyList())
   fun validate(existing: SchemaSet, newSchemas: List<VersionedSource>, removedSources: List<SchemaId> = emptyList()): Either<Pair<List<CompilationError>, List<ParsedSource>>, Pair<Schema, List<ParsedSource>>>
   fun validateAndParse(existing: SchemaSet, newVersionedSources: List<VersionedSource>, removedSources: List<SchemaId> = emptyList()): Pair<List<ParsedSource>,  Either<List<CompilationError>, Schema>>
}

