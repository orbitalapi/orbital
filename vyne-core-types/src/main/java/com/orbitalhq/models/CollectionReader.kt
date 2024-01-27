package com.orbitalhq.models

import com.orbitalhq.formats.csv.CsvAttributeAccessorParser
import com.orbitalhq.formats.csv.CsvCollectionParser
import com.orbitalhq.models.functions.FunctionRegistry
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import com.orbitalhq.schemas.fqn

/**
 * This approach is legacy, and we shouldn't build on it any further.
 */
object CollectionReader {
   private val csvReader = CsvAttributeAccessorParser()
   fun canRead(type: Type, value: Any): Boolean {
      return when {
         type.hasMetadata("CsvList".fqn()) -> true
         else -> false
      }
   }
   fun readCollectionFromNonTypedCollectionValue(type: Type,
                                                 value: Any,
                                                 schema: Schema,
                                                 source:DataSource,
                                                 functionRegistry: FunctionRegistry = FunctionRegistry.default,
                                                 inPlaceQueryEngine: InPlaceQueryEngine? = null,
                                                 metadata: Map<String, Any> = emptyMap()
                                                 ): TypedInstance {
      return when {
         type.hasMetadata("CsvList".fqn()) -> CsvCollectionParser(value as String, type, schema, source, functionRegistry, inPlaceQueryEngine, metadata).parse()
         else -> error("No reader strategy defined for collection type")
      }
   }

}
