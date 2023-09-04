package com.orbitalhq.schemaStore

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.orbitalhq.*
import com.orbitalhq.schema.api.SchemaSet
import com.orbitalhq.schema.api.SchemaValidator
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.readers.SourceToTaxiConverter
import com.orbitalhq.schemas.readers.TaxiSourceConverter
import com.orbitalhq.schemas.taxi.TaxiSchema
import lang.taxi.CompilationError
import lang.taxi.errors
import lang.taxi.sources.SourceLocation
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger { }

@Component
class TaxiSchemaValidator(
   private val sourceLoaders: List<SourceToTaxiConverter> = listOf(TaxiSourceConverter)
) :
   SchemaValidator {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   override fun validateAndParse(
      existing: SchemaSet,
      updatedPackage: SourcePackage?,
      removedPackages: List<PackageIdentifier>
   ): Pair<List<ParsedPackage>, Either<List<CompilationError>, Schema>> {
      return when (val validationResult = this.validate(existing, updatedPackage, removedPackages)) {
         is Either.Right -> {
            validationResult.value.second to validationResult.value.first.right()
         }


         is Either.Left -> {
            validationResult.value.second to validationResult.value.first.left()
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
         val (messages, schema) = TaxiSchema.fromPackages(packages, sourceConverters = sourceLoaders)
         val errors = messages.errors()
         val errorsByPackage = messages.errors().map { compilationError ->
            val compilationErrorSourceName = compilationError.sourceName
               ?: error("It should now be illegal to submit a source without a sourcename.  If this error is hit, understand the usecase. If not, lets make the field not nullable.")
            val (packageIdentifier, sourceName) = VersionedSource.splitPackageIdentifier(compilationErrorSourceName)
            Triple(packageIdentifier, sourceName, compilationError)
         }
         val parsedPackages = packages.map { sourcePackage ->
            val parsedSources = sourcePackage.sourcesWithPackageIdentifier.map { versionedSource ->
               val errors = errorsByPackage
                  .filter { error ->
                     error.first == sourcePackage.identifier && error.second == versionedSource.name
                  }
                  .map { it.third }
               ParsedSource(versionedSource, errors)
            }
            ParsedPackage(sourcePackage.packageMetadata, parsedSources, sourcePackage.additionalSources)
         }
         if (errors.isNotEmpty()) {
            logger.error("Schema contained compilation exception: \n${errors.joinToString("\n")}")
            (errors to parsedPackages).left()
         } else {
            (schema to parsedPackages).right()
         }
      } catch (exception: RuntimeException) {
         logger.error(exception) { "The compiler threw an unexpected error - this is likely a bug in the compiler. " }
         val message =
            "The compiler threw an unexpected error - this is likely a bug in the compiler - ${exception.message}"
         val parsedPackages = packages.map { sourcePackage ->
            val parsedSources = sourcePackage.sourcesWithPackageIdentifier.map {
               ParsedSource(
                  it,
                  listOf(CompilationError(SourceLocation.UNKNOWN_POSITION, message, it.name))
               )
            }
            ParsedPackage(sourcePackage.packageMetadata, parsedSources, sourcePackage.additionalSources)


         }
         val errors =
            parsedPackages.flatMap { parsedPackage -> parsedPackage.sources.flatMap { parsedSource -> parsedSource.errors } }
         (errors to parsedPackages).left()
      }
   }
}

