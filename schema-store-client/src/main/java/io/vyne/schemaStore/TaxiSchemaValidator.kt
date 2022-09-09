package io.vyne.schemaStore

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.vyne.ParsedSource
import io.vyne.SchemaId
import io.vyne.VersionedSource
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
        newVersionedSources: List<VersionedSource>,
        removedSources: List<SchemaId>
    ): Pair<List<ParsedSource>, Either<List<CompilationError>, Schema>> {
        return when (val validationResult = this.validate(existing, newVersionedSources, removedSources)) {
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
        newSchemas: List<VersionedSource>,
        removedSources: List<SchemaId>
    ): Either<Pair<List<CompilationError>, List<ParsedSource>>, Pair<Schema, List<ParsedSource>>> {
        val sources = existing.offerSources(newSchemas, removedSources)
        return try {
            // TODO : This is sloppy handling of imports, and will cause issues
            // I'm adding each schema as it's compiled into the set of available imports.
            // But, this could cause problems as schemas are removed, as a schema may reference
            // an import from a removed schema, causing all compilation to fail.
            // Need to consider this, and find a solution.
            val (messages, schema) = TaxiSchema.compiled(sources)
            val errors = messages.errors()
            if (errors.isNotEmpty()) {
                logger.error("Schema contained compilation exception: \n${errors.joinToString("\n")}")
            val errorsBySource = errors.groupBy { compilationError ->
               compilationError.sourceName
                  ?: error("It should now be illegal to submit a source without a sourcename.  If this error is hit, understand the usecase. If not, lets make the field not nullable.")
            }
            val parsedSources = sources.map { ParsedSource(it, errorsBySource.getOrDefault(it.name, emptyList())) }
                (errors to parsedSources).left()
         } else {
                (schema to sources.map { ParsedSource(it) }).right()
         }
      } catch (exception: RuntimeException) {
            logger.error(exception) { "The compiler threw an unexpected error - this is likely a bug in the compiler. " }
            val message =
                "The compiler threw an unexpected error - this is likely a bug in the compiler - ${exception.message}"
            val parsedSources = sources.map {
                ParsedSource(
                    it,
                    listOf(CompilationError(SourceLocation.UNKNOWN_POSITION, message, it.name))
                )
            }
            val errors = parsedSources.flatMap { it.errors }
            (errors to parsedSources).left()
        }
   }
}

