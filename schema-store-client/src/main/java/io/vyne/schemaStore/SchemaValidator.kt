package io.vyne.schemaStore

import arrow.core.Either
import io.vyne.CompositeSchemaBuilder
import io.vyne.ParsedSource
import io.vyne.VersionedSource
import io.vyne.schemas.Schema
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.toSourceCompilationErrors
import io.vyne.utils.log
import lang.taxi.CompilationException
import org.springframework.stereotype.Component

interface SchemaValidator {
   fun validate(existing: SchemaSet, newSchema: VersionedSource) = validate(existing, listOf(newSchema))
   fun validate(existing: SchemaSet, newSchemas: List<VersionedSource>):Either<Pair<CompilationException,List<ParsedSource>>, Pair<Schema,List<ParsedSource>>>
}

@Component
class TaxiSchemaValidator(val compositeSchemaBuilder: CompositeSchemaBuilder = CompositeSchemaBuilder()) : SchemaValidator {
   override fun validate(existing: SchemaSet, newSchemas: List<VersionedSource>): Either<Pair<CompilationException,List<ParsedSource>>, Pair<Schema,List<ParsedSource>>> {
      val sources = existing.offerSources(newSchemas)
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
         val errors = compilationException.errors.groupBy { it.sourceName ?: error("It should now be illegal to submit a source without a sourcename.  If this error is hit, understand the usecase. If not, lets make the field not nullable.") }
         val parsedSources = sources.map { ParsedSource(it, errors.getOrDefault(it.name, emptyList()).toSourceCompilationErrors()) }
         Either.left(compilationException to parsedSources)
      }
   }

}
