package io.vyne.models

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import io.vyne.schemas.AttributeName
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.Schema
import io.vyne.schemas.Type
import io.vyne.utils.log
import lang.taxi.types.PrimitiveType

@JsonDeserialize(using = TypeNamedInstanceDeserializer::class)
data class TypeNamedInstance(
   val typeName: String,
   val value: Any?
) {
   constructor(typeName: QualifiedName, value: Any?) : this(typeName.fullyQualifiedName, value)
}

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

   companion object {
      fun fromNamedType(typeNamedInstance: TypeNamedInstance, schema: Schema): TypedInstance {
         val (typeName, value) = typeNamedInstance
         val type = schema.type(typeName)
         return when {
            value == null -> TypedNull(type)
            value is Collection<*> -> {
               val collectionMemberType = getCollectionType(type, schema)
               val members = value.map { member ->
                  if (member == null) {
                     TypedNull(collectionMemberType)
                  } else {
                     fromNamedType(member as TypeNamedInstance, schema)
                  }
               }
               TypedCollection(collectionMemberType, members)
            }
            type.isScalar -> TypedValue(type, value)
            else -> createTypedObject(typeNamedInstance, schema)
         }
      }

      private fun createTypedObject(typeNamedInstance: TypeNamedInstance, schema: Schema): TypedObject {
         val type = schema.type(typeNamedInstance.typeName)
         val attributes = typeNamedInstance.value!! as Map<String, Any>
         val typedAttributes = attributes.map { (attributeName, typedInstance) ->
            when (typedInstance) {
               is TypeNamedInstance -> attributeName to fromNamedType(typedInstance,schema)
               is Collection<*> -> {
                  val collectionTypeRef = type.attributes[attributeName]?.type ?: error("Cannot look up collection type for attribute $attributeName as it is not a defined attribute on type ${type.name}")
                  val collectionType = schema.type(collectionTypeRef)
                  attributeName to TypedCollection(collectionType, typedInstance.map { fromNamedType(it as TypeNamedInstance, schema) })
               }
               else -> error("Unhandled scenario creating typedObject from TypeNamedInstance -> ${typedInstance::class.simpleName}")
            }
         }.toMap()
         return TypedObject(type,typedAttributes)
      }

      fun from(type: Type, value: Any?, schema: Schema): TypedInstance {
         return when {
            value == null -> TypedNull(type)
            value is Collection<*> -> {
               val collectionMemberType = getCollectionType(type, schema)
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
               log().warn("Using raw Array is not recommended, use a typed array instead.  Collection members are typed as Any")
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
      private val accessorReader = AccessorReader()
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
            if (field.accessor != null) {
               val attributeTypedInstance = accessorReader.read(value, field, schema)
               attributeName to attributeTypedInstance
            } else {
               val attributeValue = valueReader.read(value, attributeName)
               if (attributeValue == null) {
                  attributeName to TypedNull(schema.type(field.type))
               } else {
                  attributeName to TypedInstance.from(schema.type(field.type.name), attributeValue, schema)
               }

            }


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

   companion object {
      /**
       * Constructs a TypedCollection by interrogating the contents of the
       * provided list.
       * If the list is empty, then an exception is thrown
       */
      fun from(populatedList: List<TypedInstance>): TypedCollection {
         // TODO : Find the most compatiable abstract type.
         val first = populatedList.firstOrNull()
            ?: error("An empty list was passed, where a populated list was expected.  Cannot infer type.")
         return TypedCollection(first.type, populatedList)
      }
   }

   override fun withTypeAlias(typeAlias: Type): TypedInstance {
      return TypedCollection(typeAlias, value)
   }

   fun parameterizedType(schema: Schema): Type {
      return schema.type("lang.taxi.Array<${type.name.parameterizedName}>")
   }
}
