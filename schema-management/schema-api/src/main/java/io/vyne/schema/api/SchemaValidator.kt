package io.vyne.schema.api

import arrow.core.Either
import io.vyne.PackageIdentifier
import io.vyne.ParsedPackage
import io.vyne.ParsedSource
import io.vyne.SourcePackage
import io.vyne.schemas.Schema
import lang.taxi.CompilationError


interface SchemaValidator {
   fun validate(
      existing: SchemaSet,
      updatedPackage: SourcePackage? = null,
      removedPackages: List<PackageIdentifier> = emptyList()
   ): Either<Pair<List<CompilationError>, List<ParsedPackage>>, Pair<Schema, List<ParsedPackage>>>

   fun validateAndParse(
      existing: SchemaSet,
      updatedPackage: SourcePackage? = null,
      removedPackages: List<PackageIdentifier> = emptyList()
   ): Pair<List<ParsedPackage>, Either<List<CompilationError>, Schema>>
}

