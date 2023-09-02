package com.orbitalhq.models

import com.orbitalhq.models.format.ModelFormatSpec
import com.orbitalhq.models.functions.FunctionRegistry
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Type
import lang.taxi.types.FormatsAndZoneOffset
import lang.taxi.types.MapType
import lang.taxi.types.ObjectType
import lang.taxi.types.getUnderlyingMapType
import lang.taxi.types.isMapType

object TypedMaps {
   fun parse(
      type: Type,
      value: Any?,
      schema: Schema,
      performTypeConversions: Boolean = true,
      nullValues: Set<String> = emptySet(),
      source: DataSource = UndefinedSource,
      evaluateAccessors: Boolean = true,
      functionRegistry: FunctionRegistry = FunctionRegistry.default,
      formatSpecs: List<ModelFormatSpec> = emptyList(),
      inPlaceQueryEngine: InPlaceQueryEngine? = null,
      parsingErrorBehaviour: ParsingFailureBehaviour = ParsingFailureBehaviour.ThrowException,
      format: FormatsAndZoneOffset? = type.formatAndZoneOffset
   ): TypedInstance {
      if (value == null) {
         return TypedNull.create(type, source)
      }
      if (value !is Map<*, *>) {
         error("Cannot read type of ${value::class.simpleName} to Map")
      }
      val mapType = getMapType(type)
      val keyType = schema.type(mapType.keyType)
      val valueType = schema.type(mapType.valueType)

      val mapEntries = value.entries.map { (key,value) ->
         if (key == null) {
            error("Nulls are not supported on maps")
         }
         // TODO : For now, we'll treat TypedMaps as Map<String, TypedInstance>
         // This allows them to be coerced into an existing TypedObject, with the downside
         // that we lose the type info for the key.
         // There's likely future use-cases for having Map<TypedInstance, TypedInstance>
         // but we dont have one right now.
         val key = key.toString()
         val valueTypedInstance = TypedInstance.from(valueType, value, schema, performTypeConversions, nullValues, source, evaluateAccessors, functionRegistry, formatSpecs, inPlaceQueryEngine, parsingErrorBehaviour, format)
         key to valueTypedInstance
      }.toMap()
      return TypedObject(type, mapEntries, source)
   }

   private fun getMapType(type: Type): MapType {
      return when {
         type.taxiType is MapType -> type.taxiType
         type.taxiType is ObjectType && type.taxiType.isMapType() -> type.taxiType.getUnderlyingMapType()
         else -> error("Type ${type.qualifiedName.shortDisplayName} is not a map type")
      }
   }
}
