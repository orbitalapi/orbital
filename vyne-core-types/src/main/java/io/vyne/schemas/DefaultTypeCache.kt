package io.vyne.schemas

import io.vyne.models.*
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.utils.timed
import lang.taxi.TaxiDocument
import lang.taxi.types.EnumValueQualifiedName
import lang.taxi.types.ObjectType

abstract class BaseTypeCache : TypeCache {
   data class CachedEnumSynonymValues(
      val synonyms: List<TypedEnumValue>
   )

   private val cache: MutableMap<QualifiedName, Type> = mutableMapOf()
   private val defaultValueCache: MutableMap<QualifiedName, Map<AttributeName, TypedInstance>?> = mutableMapOf()
   private var shortNames: MutableMap<String, MutableList<Type>> = mutableMapOf()
   private val anonymousTypes: MutableMap<QualifiedName, Type> = mutableMapOf()
   private val enumSynonymValues: MutableMap<EnumValueQualifiedName, CachedEnumSynonymValues> = mutableMapOf()

   val types: Set<Type>
      get() {
         return this.cache.values.toSet()
      }

   private fun populateDefaultValuesForType(type: Type) {
      defaultValueCache[type.qualifiedName] = (type.taxiType as? ObjectType)
         ?.fields
         ?.filter { field -> field.defaultValue != null }
         ?.map { field ->
            Pair(
               field.name,
               TypedValue.from(
                  type = type(field.type.qualifiedName.fqn()),
                  value = field.defaultValue!!,
                  converter = ConversionService.DEFAULT_CONVERTER, source = DefinedInSchema
               )
            )
         }
         ?.toMap()
   }

   /**
    * Adds the type to the cache, and returns a new copy, with the
    * type cache updated.
    */
   fun add(type: Type): Type {
      val withReference = if (type.typeCache == this) type else type.copy(typeCache = this)
      cache[type.name] = withReference
      shortNames.compute(type.name.name) { _, existingList ->
         if (existingList == null) {
            mutableListOf(withReference)
         } else {
            existingList.add(withReference)
            existingList
         }
      }
      populateDefaultValuesForType(withReference)
      return withReference
   }

   override fun type(name: String): Type {
      return type(name.fqn())
   }

   override fun type(name: QualifiedName): Type {
      return typeOrNull(name)
         ?: throw IllegalArgumentException("Type ${name.parameterizedName} was not found within this schema, and is not a valid short name")
   }

   protected open fun typeOrNull(name: QualifiedName): Type? {
      return this.cache[name]
         ?: fromShortName(name)
         ?: parameterisedType(name)
         ?: anonymousTypes[name]
   }

   internal fun fromShortName(name: QualifiedName): Type? =
      this.shortNames[name.fullyQualifiedName]?.let { matchingTypes -> if (matchingTypes.size == 1) matchingTypes.first() else null }

   private fun parameterisedType(name: QualifiedName): Type? {
      if (name.parameters.isEmpty()) return null

      return if (hasType(name.fullyQualifiedName) && name.parameters.all { hasType(it) }) {
         // We've been asked for a parameterized type.
         // All the parameters are correctly defined, but no type exists.
         // This is caused by (for example), a service returning Array<Foo>, where both Array and Foo have been declared as types
         // but not Array<Foo> directly.
         // It's still valid, so we'll construct the type
         val baseType = type(name.fullyQualifiedName)
         val parameterisedType = baseType.copy(name = name, typeParametersTypeNames = name.parameters)
         add(parameterisedType)
      } else {
         null
      }

   }

   override fun hasType(name: QualifiedName): Boolean {
      if (cache.containsKey(name)) return true
      if (anonymousTypes.containsKey(name)) return true
      if (name.parameters.isNotEmpty()) {
         return hasType(name.fullyQualifiedName) // this is the base type
            && name.parameters.all { hasType(it) }
      }
      return false
   }

   override fun defaultValues(name: QualifiedName): Map<AttributeName, TypedInstance>? {
      return defaultValueCache[name]
   }

   override fun registerAnonymousType(anonymousType: Type) {
      val withReference = anonymousType.copy(typeCache = this)
      anonymousTypes[anonymousType.qualifiedName] = withReference
   }

   override fun anonymousTypes(): Set<Type> {
      return this.anonymousTypes.values.toSet()
   }

   private fun getEnumSynonyms(typedEnumValue: TypedEnumValue): CachedEnumSynonymValues {
      return this.enumSynonymValues.getOrPut(typedEnumValue.enumValueQualifiedName) {

         val synonymTypedValues = typedEnumValue.enumValue.synonyms.map { synonymName ->
            val synonymQualifiedName = synonymName.synonymFullyQualifiedName()
            val synonymEnumValue = synonymName.synonymValue()
            val synonymEnumType = this.type(synonymQualifiedName)
            synonymEnumType.enumTypedInstance(synonymEnumValue, source = DefinedInSchema)
         }.toList()
         CachedEnumSynonymValues(synonymTypedValues)
      }
   }

   override fun enumSynonyms(typedEnumValue: TypedEnumValue): List<TypedEnumValue> {
      return getEnumSynonyms(typedEnumValue).synonyms
   }

   private val isAssignableWithTypeParameters = mutableMapOf<String, Boolean>()
   private val isAssignableWithoutTypeParameters = mutableMapOf<String, Boolean>()
   override fun isAssignable(
      typeA: Type,
      typeB: Type,
      considerTypeParameters: Boolean,
      func: (Type, Type, Boolean) -> Boolean
   ): Boolean {
      val key = typeA.fullyQualifiedName + "-[isAssignableTo]->" + typeB.fullyQualifiedName
      return if (considerTypeParameters) {
         isAssignableWithTypeParameters.getOrPut(key) { func(typeA, typeB, considerTypeParameters) }
      } else {
         isAssignableWithoutTypeParameters.getOrPut(key) { func(typeA, typeB, considerTypeParameters) }
      }
   }

   override fun hasType(name: String): Boolean {
      return shortNames[name]?.size == 1 || hasType(name.fqn())
   }
}

/**
 * Simple TypeCache which takes a set of types.
 * Upon creation, the types are copied into this type cache, with thier
 * internal typeCache property updated to this cache
 */
class DefaultTypeCache(types: Set<Type> = emptySet()) : BaseTypeCache() {

   init {
      timed("DefaultTypeCache initialisation") {
         types.forEach { add(it) }
      }
   }
}

/**
 * A type cache which can on-demand populate it's values
 * from an underlying Taxi schema
 */
class TaxiTypeCache(private val taxi: TaxiDocument, private val schema: Schema) : BaseTypeCache() {
   init {
      TaxiSchema.taxiPrimitiveTypes.forEach { add(it) }
      taxi.types.forEach {
         addTaxiType(it)
      }
   }

   override fun hasType(name: String): Boolean {
      return super.hasType(name) || taxi.containsType(name)
   }

   override fun typeOrNull(name: QualifiedName): Type {
      val fromBaseCache = super.typeOrNull(name);
      if (fromBaseCache != null) {
         return fromBaseCache
      }
      val taxiType = taxi.type(name.parameterizedName)
      return addTaxiType(taxiType)

   }

   private fun addTaxiType(taxiType: lang.taxi.types.Type): Type {
      val type = TaxiTypeMapper.fromTaxiType(taxiType, schema, this)
      return add(type)
   }
}
