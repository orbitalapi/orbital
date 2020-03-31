package io.vyne.models

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import io.vyne.schemas.*
import io.vyne.utils.log
import lang.taxi.jvm.common.PrimitiveTypes
import lang.taxi.types.PrimitiveType
import org.springframework.core.convert.support.DefaultConversionService

@JsonDeserialize(using = TypeNamedInstanceDeserializer::class)
data class TypeNamedInstance(
   val typeName: String,
   val value: Any?
) {
   constructor(typeName: QualifiedName, value: Any?) : this(typeName.fullyQualifiedName, value)
}

data class VersionedTypedInstance(
   val type: VersionedType,
   val instance: TypedInstance
)

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
      fun fromNamedType(typeNamedInstance: TypeNamedInstance, schema: Schema, performTypeConversions:Boolean = true): TypedInstance {
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

      fun from(type: Type, value: Any?, schema: Schema, performTypeConversions: Boolean = true): TypedInstance {
         return when {
            value is TypedInstance -> value
            value == null -> TypedNull(type)
            value is Collection<*> -> {
               val collectionMemberType = getCollectionType(type, schema)
               TypedCollection(collectionMemberType, value.filterNotNull().map { from(collectionMemberType, it, schema, performTypeConversions) })
            }
            type.isScalar -> {
               TypedValue.from(type, value, performTypeConversions)
            }
            // This is a bit special...value isn't a collection, but the type is.  Oooo!
            // Must be a CSV ish type value.
            type.isCollection -> readCollectionTypeFromNonCollectionValue(type, value, schema)
            else -> TypedObject.fromValue(type, value, schema)
         }
      }

      private fun readCollectionTypeFromNonCollectionValue(type: Type, value: Any, schema: Schema): TypedInstance {
         return CollectionReader.readCollectionFromNonTypedCollectionValue(type, value, schema)
      }

      private fun getCollectionType(type: Type, schema: Schema): Type {
         if (type.resolvesSameAs(schema.type(PrimitiveType.ARRAY.qualifiedName))) {
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

   override fun valueEquals(valueToCompare: TypedInstance): Boolean {
      return valueToCompare.value == null
   }
}

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

data class TypedValue private constructor(override val type: Type, override val value: Any) : TypedInstance {
   companion object {
      private val conversionService = DefaultConversionService()
      fun from(type: Type, value: Any, performTypeConversions: Boolean = true): TypedValue {
         val valueToUse = if (performTypeConversions) {
            if (!type.taxiType.inheritsFromPrimitive) {
               error("Type is not a primitive, cannot be converted")
            } else {
               conversionService.convert(value, PrimitiveTypes.getJavaType(type.taxiType.basePrimitive!!))
            }
         } else {
            value
         }
         return TypedValue(type, valueToUse)
      }
   }

   override fun withTypeAlias(typeAlias: Type): TypedInstance {
      return TypedValue(typeAlias, value)
   }

   /**
    * Returns true if the two are equal, where the values are the same, and the underlying
    * types resolve to the same type, considering type aliases.
    */
   override fun valueEquals(valueToCompare: TypedInstance): Boolean {
      if (valueToCompare !is TypedValue) {
         return false
      }
      if (!(this.type.resolvesSameAs(valueToCompare.type) || valueToCompare.type.inheritsFrom(this.type))) return false;
      return this.value == valueToCompare.value
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

   override fun valueEquals(valueToCompare: TypedInstance): Boolean {
      if (valueToCompare !is TypedCollection) {
         return false
      }
      if (!this.type.resolvesSameAs(valueToCompare.type)) {
         return false
      }
      if (this.size != valueToCompare.size) {
         return false
      }
      this.forEachIndexed { index, typedInstance ->
         if (!typedInstance.valueEquals(valueToCompare[index])) {
            // Fail as soon as any values don't equal
            return false
         }
      }
      return true
   }

   fun parameterizedType(schema: Schema): Type {
      return schema.type("lang.taxi.Array<${type.name.parameterizedName}>")
   }
}
