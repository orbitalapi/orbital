package io.polymer.schemaStore

import com.diffplug.common.base.Either
import io.osmosis.polymer.CompositeSchemaBuilder
import io.osmosis.polymer.schemas.Schema
import io.osmosis.polymer.schemas.taxi.TaxiSchema
import lang.taxi.CompilationException

interface SchemaValidator {
   fun validate(existing: SchemaSet, newSchema: VersionedSchema): Either<CompilationException, Schema>
}

class TaxiSchemaValidator(val compositeSchemaBuilder: CompositeSchemaBuilder = CompositeSchemaBuilder()) : SchemaValidator {
   override fun validate(existing: SchemaSet, newSchema: VersionedSchema): Either<CompilationException, Schema> {
      return try {
         val schemas = (existing.schemas + newSchema).map { TaxiSchema.from(it.content, it.id) }
         Either.createRight(compositeSchemaBuilder.aggregate(schemas))
      } catch (e: CompilationException) { // other exceptions are thrown
         Either.createLeft(e)
      }
   }

}
