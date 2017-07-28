package io.osmosis.polymer.models

import io.osmosis.polymer.schemas.AttributeName
import io.osmosis.polymer.schemas.Schema
import io.osmosis.polymer.schemas.Type

interface TypedInstance {
   val type: Type
   val value: Any

   companion object {
      fun from(type: Type, value: Any, schema: Schema): TypedInstance {
         if (value is Collection<*>) {
            return TypedCollection(type, value.filterNotNull().map { from(type, it, schema) })
         } else if (type.isScalar) {
            return TypedValue(type, value)
         } else {
            return TypedObject.fromValue(type, value, schema)
         }
      }
   }
}

data class TypedObject(override val type: Type, override val value: Map<String, TypedInstance>) : TypedInstance, Map<String, TypedInstance> by value {
   companion object {
      fun fromValue(type: Type, value: Any, schema: Schema): TypedObject {
         val attributes: Map<AttributeName, TypedInstance> = type.attributes.map { (attributeName, attributeType) ->
            val field = value.javaClass.getField(attributeName)
            val attributeValue = field.get(value)
            attributeName to TypedInstance.from(schema.type(attributeType.name), attributeValue, schema)
         }.toMap()
         return TypedObject(type, attributes)
      }
   }

   fun hasAttribute(name: String): Boolean {
      return this.value.containsKey(name)
   }
}

data class TypedValue(override val type: Type, override val value: Any) : TypedInstance
data class TypedCollection(override val type: Type, override val value: List<TypedInstance>) : List<TypedInstance> by value, TypedInstance
