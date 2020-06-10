package io.vyne.models

import io.vyne.models.json.JsonModelParser
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
      return TypedInstanceConverter(RawObjectMapper()).convert(this)
//      return convert { it.value }
   }

   fun toTypeNamedInstance(): Any? {
      return TypedInstanceConverter(TypeNamedInstanceMapper()).convert(this)
   }

   fun valueEquals(valueToCompare: TypedInstance): Boolean

   companion object {
      fun fromNamedType(typeNamedInstance: TypeNamedInstance, schema: Schema, performTypeConversions: Boolean = true): TypedInstance {
         val (typeName, value) = typeNamedInstance
         val type = schema.type(typeName)
         return when {
            value == null -> TypedNull(type)
            value is Collection<*> -> {
               val collectionMemberType = getCollectionType(type)
               val members = value.map { member ->
                  if (member == null) {
                     TypedNull(collectionMemberType)
                  } else {
                     fromNamedType(member as TypeNamedInstance, schema, performTypeConversions)
                  }
               }
               TypedCollection(collectionMemberType, members)
            }
            type.isScalar -> TypedValue.from(type, value, performTypeConversions)
            else -> createTypedObject(typeNamedInstance, schema, performTypeConversions)
         }
      }

      private fun createTypedObject(typeNamedInstance: TypeNamedInstance, schema: Schema, performTypeConversions: Boolean): TypedObject {
         val type = schema.type(typeNamedInstance.typeName)
         val attributes = typeNamedInstance.value!! as Map<String, Any>
         val typedAttributes = attributes.map { (attributeName, typedInstance) ->
            when (typedInstance) {
               is TypeNamedInstance -> attributeName to fromNamedType(typedInstance, schema, performTypeConversions)
               is Collection<*> -> {
                  val collectionTypeRef = type.attributes[attributeName]?.type
                     ?: error("Cannot look up collection type for attribute $attributeName as it is not a defined attribute on type ${type.name}")
                  val collectionType = schema.type(collectionTypeRef)
                  attributeName to TypedCollection(collectionType, typedInstance.map { fromNamedType(it as TypeNamedInstance, schema, performTypeConversions) })
               }
               else -> error("Unhandled scenario creating typedObject from TypeNamedInstance -> ${typedInstance::class.simpleName}")
            }
         }.toMap()
         return TypedObject(type, typedAttributes)
      }

      fun from(type: Type, value: Any?, schema: Schema, performTypeConversions: Boolean = true, nullValues: Set<String> = emptySet()): TypedInstance {
         return when {
            value is TypedInstance -> value
            value == null -> TypedNull(type)
            value is Collection<*> -> {
               val collectionMemberType = getCollectionType(type)
               TypedCollection.arrayOf(collectionMemberType, value.filterNotNull().map { from(collectionMemberType, it, schema, performTypeConversions) })
            }
            type.isScalar -> {
               TypedValue.from(type, value, performTypeConversions)
            }
            isJson(value) -> JsonModelParser(schema).parse(type,value as String)

            // This is a bit special...value isn't a collection, but the type is.  Oooo!
            // Must be a CSV ish type value.
            type.isCollection -> readCollectionTypeFromNonCollectionValue(type, value, schema)
            else -> TypedObject.fromValue(type, value, schema, nullValues)
         }
      }

      private fun readCollectionTypeFromNonCollectionValue(type: Type, value: Any, schema: Schema): TypedInstance {
         return CollectionReader.readCollectionFromNonTypedCollectionValue(type, value, schema)
      }

      private fun getCollectionType(type: Type): Type {
         return if (type.isCollection) {
            type.collectionType ?: error("Type should return a collection when isCollection is true.")
         } else {
            log().warn("Collection type could not be determined - expected to find ${PrimitiveType.ARRAY.qualifiedName}, but found ${type.fullyQualifiedName}")
            type
         }
      }

      private fun isJson(value: Any): Boolean {
         if (value !is String) return false;
         val trimmed = value.trim()
         return when {
            trimmed.startsWith("{") && trimmed.endsWith("}") -> true
            trimmed.startsWith("[") && trimmed.endsWith("]") -> true
            else -> false
         }
      }
   }
}
