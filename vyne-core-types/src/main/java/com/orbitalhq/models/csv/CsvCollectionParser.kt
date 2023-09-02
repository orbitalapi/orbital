package com.orbitalhq.models.csv

import com.orbitalhq.models.DataSource
import com.orbitalhq.models.InPlaceQueryEngine
import com.orbitalhq.models.TypedCollection
import com.orbitalhq.models.TypedInstance
import com.orbitalhq.models.TypedObjectFactory
import com.orbitalhq.models.functions.FunctionRegistry
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type

class CsvCollectionParser(val content: String, val type: Type, val schema: Schema, val source:DataSource, val functionRegistry: FunctionRegistry = FunctionRegistry.default, val inPlaceQueryEngine: InPlaceQueryEngine? = null) {
   private val memberType: Type

   init {
      require(this.type.isCollection) { "The passed type should be a collection type" }
      require(this.type.resolveAliases().typeParameters.size == 1) { "The collection type should contain exactly 1 type param" }
      this.memberType = this.type.resolveAliases().typeParameters.first()
   }

   fun parse(): TypedInstance {
      val typedInstances = content.lineSequence()
         .drop(1) // Ignore the header
         .filter { it.isNotBlank() && it.isNotEmpty() }
         .map { TypedObjectFactory(memberType,it,schema, source = source, functionRegistry = functionRegistry, inPlaceQueryEngine = inPlaceQueryEngine, formatSpecs = emptyList()).build() }
         .toList()
      return TypedCollection.from(typedInstances, source)
   }

}
