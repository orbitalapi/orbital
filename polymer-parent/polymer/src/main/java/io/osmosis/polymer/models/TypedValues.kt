package io.osmosis.polymer.models

import io.osmosis.polymer.schemas.AttributeName
import io.osmosis.polymer.schemas.Schema
import io.osmosis.polymer.schemas.Type

interface TypedInstance {
   val type: Type
   val value: Any

   fun toRawObject():Any {

      val unwrapMap = { valueMap:Map<String,Any> -> valueMap.map { (entryKey, entryValue) ->
            when (entryValue) {
               is TypedInstance -> entryKey to entryValue.value
               else -> entryKey to entryValue
            }
         }.toMap()
      }

      when (value) {
         is Map<*,*> -> return unwrapMap(value as Map<String,Any>)
         // TODO : There's likely other types that need unwrapping
         else -> return value
      }
   }

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
      fun fromValue(typeName: String, value: Any, schema: Schema): TypedObject {
         return fromValue(schema.type(typeName), value, schema)
      }

      fun fromAttributes(typeName: String, attributes: Map<String, Any>, schema: Schema): TypedObject {
         return fromAttributes(schema.type(typeName), attributes, schema)
      }

      fun fromAttributes(type: Type, attributes: Map<String, Any>, schema: Schema): TypedObject {
         val typedAttributes: Map<String, TypedInstance> = attributes.map { (attributeName, value) ->
            val attributeType = schema.type(type.attributes[attributeName]!!)
            attributeName to TypedInstance.from(attributeType, value, schema)
         }.toMap()
         return TypedObject(type, typedAttributes)
      }

      fun fromValue(type: Type, value: Any, schema: Schema): TypedObject {
         val attributes: Map<AttributeName, TypedInstance> = type.attributes.map { (attributeName, attributeType) ->
            // TODO : DeclaredFields / Fields doesn't work - swap this with a reflection library that will handle both
            val field = value.javaClass.getDeclaredField(attributeName)
            field.isAccessible = true
            val attributeValue = field.get(value)
            attributeName to TypedInstance.from(schema.type(attributeType.name), attributeValue, schema)
         }.toMap()
         return TypedObject(type, attributes)
      }
   }

   fun hasAttribute(name: String): Boolean {
      return this.value.containsKey(name)
   }

   fun getObject(key: String): TypedObject {
      return get(key) as TypedObject
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

   fun copy(replacingArgs: Map<AttributeName, TypedInstance>): TypedObject {
      return TypedObject(this.type, this.value + replacingArgs)
   }
}

data class TypedValue(override val type: Type, override val value: Any) : TypedInstance
data class TypedCollection(override val type: Type, override val value: List<TypedInstance>) : List<TypedInstance> by value, TypedInstance
