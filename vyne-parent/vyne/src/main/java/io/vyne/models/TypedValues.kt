package io.vyne.models

import io.vyne.schemas.AttributeName
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.utils.log
import lang.taxi.types.PrimitiveType

interface TypedInstance {
   val type: Type
   val value: Any?

   // It's up to instances of this to reconstruct themselves with their type
   // set to the value of the typeAlias.
   fun withTypeAlias(typeAlias: Type): TypedInstance

   fun toRawObject(): Any? {

      val unwrapMap = { valueMap: Map<String, Any> ->
         valueMap.map { (entryKey, entryValue) ->
            when (entryValue) {
               is TypedInstance -> entryKey to entryValue.toRawObject()
               else -> entryKey to entryValue
            }
         }.toMap()
      }

      val unwrapCollection = { valueCollection: Collection<*> ->
         valueCollection.map { collectionMember ->
            when (collectionMember) {
               is TypedInstance -> collectionMember.toRawObject()
               else -> collectionMember
            }
         }
      }

      return when (value) {
         null -> null
         is Map<*, *> -> unwrapMap(value as Map<String, Any>)
         is Collection<*> -> unwrapCollection(value as Collection<*>)
         // TODO : There's likely other types that need unwrapping
         else -> value
      }
   }

   companion object {
      fun from(type: Type, value: Any?, schema: Schema): TypedInstance {
         return when {
            value == null -> TypedNull(type)
            value is Collection<*> -> {
               val collectionMemberType = getCollectionType(type,schema)
               TypedCollection(collectionMemberType, value.filterNotNull().map { from(collectionMemberType, it, schema) })
            }
            type.isScalar -> TypedValue(type, value)
            else -> TypedObject.fromValue(type, value, schema)
         }
      }

      private fun getCollectionType(type: Type, schema: Schema): Type {
         if (type.fullyQualifiedName == PrimitiveType.ARRAY.qualifiedName) {
            if (type.typeParameters.size == 1) {
               return type.typeParameters[0]
            } else {
               log().warn("Using raw Array is not recommenced, use a typed array instead.  Collection members are typed as Any")
               return schema.type(PrimitiveType.ANY.qualifiedName)
            }
         } else {
            log().warn("Collection type could not be determined - expected to find ${PrimitiveType.ARRAY.qualifiedName}, but found ${type.fullyQualifiedName}")
            return type
         }
      }
   }
}

data class TypedNull(override val type: Type) : TypedInstance {
   override val value: Any? = null
   override fun withTypeAlias(typeAlias: Type): TypedInstance {
      return TypedNull(typeAlias)
   }
}

data class TypedObject(override val type: Type, override val value: Map<String, TypedInstance>) : TypedInstance, Map<String, TypedInstance> by value {
   companion object {
      private val valueReader = ValueReader()
      fun fromValue(typeName: String, value: Any, schema: Schema): TypedObject {
         return fromValue(schema.type(typeName), value, schema)
      }

      fun fromAttributes(typeName: String, attributes: Map<String, Any>, schema: Schema): TypedObject {
         return fromAttributes(schema.type(typeName), attributes, schema)
      }

      fun fromAttributes(type: Type, attributes: Map<String, Any>, schema: Schema): TypedObject {
         val typedAttributes: Map<String, TypedInstance> = attributes.map { (attributeName, value) ->
            val attributeType = schema.type(type.attributes.getValue(attributeName).type)
            attributeName to TypedInstance.from(attributeType, value, schema)
         }.toMap()
         return TypedObject(type, typedAttributes)
      }

      fun fromValue(type: Type, value: Any, schema: Schema): TypedObject {
         val attributes: Map<AttributeName, TypedInstance> = type.attributes.map { (attributeName, field) ->
            val attributeValue = valueReader.read(value, attributeName) ?: TODO("Null values not yet supported")
            attributeName to TypedInstance.from(schema.type(field.type.name), attributeValue, schema)
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

   override fun withTypeAlias(typeAlias: Type): TypedInstance {
      return TypedObject(typeAlias, value)
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

data class TypedValue(override val type: Type, override val value: Any) : TypedInstance {
   override fun withTypeAlias(typeAlias: Type): TypedInstance {
      return TypedValue(typeAlias, value)
   }

   /**
    * Returns true if the two are equal, where the values are the same, and the underlying
    * types resolve to the same type, considering type aliases.
    */
   fun valueEquals(other: TypedValue): Boolean {
      if (!this.type.resolvesSameAs(other.type)) return false;
      return this.value == other.value
   }

}

data class TypedCollection(override val type: Type, override val value: List<TypedInstance>) : List<TypedInstance> by value, TypedInstance {
   override fun withTypeAlias(typeAlias: Type): TypedInstance {
      return TypedCollection(typeAlias, value)
   }

   fun parameterizedType(schema: Schema): Type {
      return schema.type("lang.taxi.Array<${type.name.parameterizedName}>")
   }
}
