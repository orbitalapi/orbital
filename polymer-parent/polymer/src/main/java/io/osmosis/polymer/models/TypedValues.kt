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

   // TODO : Needs a test
   override operator fun get(key: String): TypedInstance {
      val parts = key.split(".").toMutableList()
      val thisFieldName = parts.removeAt(0)
      val attributeValue = this.value[thisFieldName] ?: error("No attribute named $thisFieldName found on this type (${type.name})")

      if (parts.isEmpty()) {
         return attributeValue
      } else {
         val remainingAccessor = parts.joinToString(".")
         if (attributeValue is TypedObject) {
            return attributeValue[remainingAccessor]
         } else {
            throw IllegalArgumentException("Cannot evaluate an accessor ($remainingAccessor) as value is not an object with fields (${attributeValue.type.name})")
         }
      }
   }
}

data class TypedValue(override val type: Type, override val value: Any) : TypedInstance
data class TypedCollection(override val type: Type, override val value: List<TypedInstance>) : List<TypedInstance> by value, TypedInstance
