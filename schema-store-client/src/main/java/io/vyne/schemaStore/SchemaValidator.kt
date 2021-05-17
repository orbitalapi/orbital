package io.vyne.schemaStore

import arrow.core.Either
import io.vyne.*
import io.vyne.schemas.Schema
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.utils.log
import lang.taxi.CompilationError
import lang.taxi.CompilationException
import lang.taxi.sources.SourceLocation
import org.springframework.stereotype.Component

interface SchemaValidator {
   fun validate(existing: SchemaSet, newSchema: VersionedSource) = validate(existing, listOf(newSchema), emptyList())
   fun validate(existing: SchemaSet, newSchemas: List<VersionedSource>, removedSources: List<SchemaId>): Either<Pair<CompilationException, List<ParsedSource>>, Pair<Schema, List<ParsedSource>>>
   fun validateAndParse(existing: SchemaSet, newVersionedSources: List<VersionedSource>, removedSources: List<SchemaId>): Pair<List<ParsedSource>,  Either<CompilationException, Schema>>
}

@Component
class TaxiSchemaValidator(val compositeSchemaBuilder: CompositeSchemaBuilder = CompositeSchemaBuilder()) : SchemaValidator {
   override fun validateAndParse(existing: SchemaSet, newVersionedSources: List<VersionedSource>, removedSources: List<SchemaId>): Pair<List<ParsedSource>,  Either<CompilationException, Schema>> {
      return when (val validationResult = this.validate(existing, newVersionedSources, removedSources)) {
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
      newSchemas: List<VersionedSource>,
      removedSources: List<SchemaId>): Either<Pair<CompilationException, List<ParsedSource>>, Pair<Schema, List<ParsedSource>>> {
      val sources =  existing.offerSources(newSchemas, removedSources)
      return try {
         // TODO : This is sloppy handling of imports, and will cause issues
         // I'm adding each schema as it's compiled into the set of available imports.
         // But, this could cause problems as schemas are removed, as a schema may reference
         // an import from a removed schema, causing all compilation to fail.
         // Need to consider this, and find a solution.
         val schema = TaxiSchema.from(sources)
         Either.right(schema to sources.map { ParsedSource(it) })
      } catch (compilationException: CompilationException) { // other exceptions are thrown
         log().error("Schema contained compilation exception: \n${compilationException.message}")
         val errors = compilationException.errors.groupBy { compilationError ->
            compilationError.sourceName
               ?: error("It should now be illegal to submit a source without a sourcename.  If this error is hit, understand the usecase. If not, lets make the field not nullable.")
         }
         val parsedSources = sources.map { ParsedSource(it, errors.getOrDefault(it.name, emptyList())) }
         Either.left(compilationException to parsedSources)
      } catch (exception: RuntimeException) {
         log().error("The compiler threw an unexpected error - this is likely a bug in the compiler. ", exception)
         val message = "The compiler threw an unexpected error - this is likely a bug in the compiler - ${exception.message}"
         val parsedSources = sources.map { ParsedSource(it, listOf(CompilationError(SourceLocation.UNKNOWN_POSITION, message, it.name))) }
         val compilationException = CompilationException(errors = listOf(CompilationError(SourceLocation.UNKNOWN_POSITION, message)))
         Either.left(compilationException to parsedSources)
      }
   }

}
