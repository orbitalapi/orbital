package io.vyne.models

import io.vyne.schemas.AttributeName
import io.vyne.schemas.Schema
import io.vyne.schemas.Type


data class TypedObject(override val type: Type, override val value: Map<String, TypedInstance>) : TypedInstance, Map<String, TypedInstance> by value {
   companion object {
      fun fromValue(typeName: String, value: Any, schema: Schema): TypedObject {
         return fromValue(schema.type(typeName), value, schema)
      }

      fun fromAttributes(typeName: String, attributes: Map<String, Any>, schema: Schema, performTypeConversions: Boolean = true): TypedObject {
         return fromAttributes(schema.type(typeName), attributes, schema, performTypeConversions)
      }

      fun fromAttributes(type: Type, attributes: Map<String, Any>, schema: Schema, performTypeConversions: Boolean = true): TypedObject {
         val typedAttributes: Map<String, TypedInstance> = attributes.map { (attributeName, value) ->
            val attributeType = schema.type(type.attributes.getValue(attributeName).type)
            attributeName to TypedInstance.from(attributeType, value, schema, performTypeConversions)
         }.toMap()
         return TypedObject(type, typedAttributes)
      }

      fun fromValue(type: Type, value: Any, schema: Schema): TypedObject {
         return TypedObjectFactory(type, value, schema).build()
      }
   }

   fun hasAttribute(name: String): Boolean {
      return this.value.containsKey(name)
   }

   fun getObject(key: String): TypedObject {
      return get(key) as TypedObject
   }

   override fun withTypeAlias(typeAlias: Type): TypedInstance {
      return TypedObject(typeAlias, value)
   }

   override fun valueEquals(valueToCompare: TypedInstance): Boolean {
      if (valueToCompare !is TypedObject) {
         return false
      }
      if (!this.type.resolvesSameAs(valueToCompare.type)) {
         return false
      }
      return this.value.all { (attributeName, value) ->
         valueToCompare.hasAttribute(attributeName) && valueToCompare.get(attributeName).valueEquals(value)
      }
   }

   // TODO : Needs a test
   override operator fun get(key: String): TypedInstance {
      val parts = key.split(".").toMutableList()
      val thisFieldName = parts.removeAt(0)
      val attributeValue = this.value[thisFieldName]
         ?: error("No attribute named $thisFieldName found on this type (${type.name})")

      return if (parts.isEmpty()) {
         attributeValue
      } else {
         val remainingAccessor = parts.joinToString(".")
         if (attributeValue is TypedObject) {
            attributeValue[remainingAccessor]
         } else {
            throw IllegalArgumentException("Cannot evaluate an accessor ($remainingAccessor) as value is not an object with fields (${attributeValue.type.name})")
         }
      }
   }

   fun copy(replacingArgs: Map<AttributeName, TypedInstance>): TypedObject {
      return TypedObject(this.type, this.value + replacingArgs)
   }
}
