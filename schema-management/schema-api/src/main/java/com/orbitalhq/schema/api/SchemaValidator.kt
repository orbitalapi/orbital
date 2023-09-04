package com.orbitalhq.schema.api

import arrow.core.Either
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.ParsedPackage
import com.orbitalhq.ParsedSource
import com.orbitalhq.SourcePackage
import com.orbitalhq.schemas.Schema
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

