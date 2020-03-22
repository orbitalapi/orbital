package io.vyne.schemaStore

import io.vyne.CompositeSchemaBuilder
import io.vyne.schemas.Schema
import io.vyne.schemas.taxi.TaxiSchema
import lang.taxi.CompilationException
import org.funktionale.either.Either
import org.springframework.stereotype.Component

interface SchemaValidator {
   fun validate(existing: SchemaSet, newSchema: VersionedSchema) = validate(existing, listOf(newSchema))
   fun validate(existing: SchemaSet, newSchemas: List<VersionedSchema>): Either<CompilationException, Schema>
}

@Component
class TaxiSchemaValidator(val compositeSchemaBuilder: CompositeSchemaBuilder = CompositeSchemaBuilder()) : SchemaValidator {
   override fun validate(existing: SchemaSet, newSchemas: List<VersionedSchema>): Either<CompilationException, Schema> {
      return try {
         val schemaSet = existing.add(newSchemas)
         // TODO : This is sloppy handling of imports, and will cause issues
         // I'm adding each schema as it's compiled into the set of available imports.
         // But, this could cause problems as schemas are removed, as a schema may reference
         // an import from a removed schema, causing all compilation to fail.
         // Need to consider this, and find a solution.
         val schema = TaxiSchema.from(schemaSet.namedSources)
         Either.right(schema)
      } catch (e: CompilationException) { // other exceptions are thrown
         Either.left(e)
      }
   }

}
