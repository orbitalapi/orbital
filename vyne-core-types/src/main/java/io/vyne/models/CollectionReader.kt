package io.vyne.models

import io.vyne.models.csv.CsvAttributeAccessorParser
import io.vyne.models.csv.CsvCollectionParser
import io.vyne.models.functions.FunctionRegistry
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.schemas.fqn

object CollectionReader {
   private val csvReader = CsvAttributeAccessorParser()
   fun readCollectionFromNonTypedCollectionValue(type: Type, value: Any, schema: Schema, source:DataSource, functionRegistry: FunctionRegistry = FunctionRegistry.default, inPlaceQueryEngine: InPlaceQueryEngine? = null): TypedInstance {
      return when {
         type.hasMetadata("CsvList".fqn()) -> CsvCollectionParser(value as String, type, schema, source, functionRegistry, inPlaceQueryEngine).parse()
         else -> error("No reader strategy defined for collection type")
      }
   }

}
