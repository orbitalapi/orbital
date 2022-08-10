package io.vyne.schemaStore

import arrow.core.Either
import io.vyne.*
import io.vyne.schema.api.SchemaSet
import io.vyne.schema.api.SchemaValidator
import io.vyne.schemas.Schema
import io.vyne.schemas.taxi.TaxiSchema
import lang.taxi.CompilationError
import lang.taxi.errors
import lang.taxi.sources.SourceLocation
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger { }

@Component
class TaxiSchemaValidator :
   SchemaValidator {
   override fun validateAndParse(
      existing: SchemaSet,
      updatedPackage: SourcePackage?,
      removedPackages: List<PackageIdentifier>
   ): Pair<List<ParsedPackage>, Either<List<CompilationError>, Schema>> {
      return when (val validationResult = this.validate(existing, updatedPackage, removedPackages)) {
         is Either.Right -> {
            validationResult.b.second to Either.right(validationResult.b.first)
         }

         is Either.Left -> {
            validationResult.a.second to Either.left(validationResult.a.first)
         }
      }
   }

   override fun validate(
      existing: SchemaSet,
      updatedPackage: SourcePackage?,
      removedPackages: List<PackageIdentifier>
   ): Either<Pair<List<CompilationError>, List<ParsedPackage>>, Pair<Schema, List<ParsedPackage>>> {
      val packages = existing.getPackagesAfterUpdate(updatedPackage, removedPackages)
      return try {
         // TODO : This is sloppy handling of imports, and will cause issues
         // I'm adding each schema as it's compiled into the set of available imports.
         // But, this could cause problems as schemas are removed, as a schema may reference
         // an import from a removed schema, causing all compilation to fail.
         // Need to consider this, and find a solution.
         val (messages, schema) = TaxiSchema.fromPackages(packages)
         val errors = messages.errors()
         val errorsByPackage = messages.errors().map { compilationError ->
            val compilationErrorSourceName = compilationError.sourceName
               ?: error("It should now be illegal to submit a source without a sourcename.  If this error is hit, understand the usecase. If not, lets make the field not nullable.")
            val (packageIdentifier, sourceName) = VersionedSource.splitPackageIdentifier(compilationErrorSourceName)
            Triple(packageIdentifier, sourceName, compilationError)
         }
         val parsedPackages = packages.map { sourcePackage ->
            val parsedSources = sourcePackage.sources.map { versionedSource ->
               val errors = errorsByPackage
                  .filter { error ->
                     error.first == sourcePackage.identifier && error.second == versionedSource.name
                  }
                  .map { it.third }
               ParsedSource(versionedSource, errors)
            }
            ParsedPackage(sourcePackage.packageMetadata, parsedSources)
         }
         if (errors.isNotEmpty()) {
            logger.error("Schema contained compilation exception: \n${errors.joinToString("\n")}")
            Either.left(errors to parsedPackages)
         } else {
            Either.right(schema to parsedPackages)
         }
      } catch (exception: RuntimeException) {
         logger.error(exception) { "The compiler threw an unexpected error - this is likely a bug in the compiler. " }
         val message =
            "The compiler threw an unexpected error - this is likely a bug in the compiler - ${exception.message}"
         val parsedPackages = packages.map { sourcePackage ->
            val parsedSources = sourcePackage.sources.map {
               ParsedSource(
                  it,
                  listOf(CompilationError(SourceLocation.UNKNOWN_POSITION, message, it.name))
               )
            }
            ParsedPackage(sourcePackage.packageMetadata, parsedSources)


         }
         val errors =
            parsedPackages.flatMap { parsedPackage -> parsedPackage.sources.flatMap { parsedSource -> parsedSource.errors } }
         Either.left(errors to parsedPackages)
      }
   }
}

