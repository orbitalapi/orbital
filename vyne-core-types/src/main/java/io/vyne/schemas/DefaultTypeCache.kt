package io.vyne.schemas

import io.vyne.models.*
import io.vyne.utils.timed
import lang.taxi.types.EnumValueQualifiedName
import lang.taxi.types.ObjectType

class DefaultTypeCache(types: Set<Type> = emptySet()) : TypeCache {
   data class CachedEnumSynonymValues(
      val asName: List<TypedValue>,
      val asValue: List<TypedValue>,
      val synonyms: List<TypedEnumValue>
   ) {
      fun get(valueKind: EnumValueKind): List<TypedValue> {
         return when (valueKind) {
            EnumValueKind.VALUE -> asValue
            EnumValueKind.NAME -> asName
         }
      }
   }

   private val cache: MutableMap<QualifiedName, Type> = mutableMapOf()
   private val defaultValueCache: MutableMap<QualifiedName, Map<AttributeName, TypedInstance>?> = mutableMapOf()
   private var shortNames: MutableMap<String, MutableList<Type>> = mutableMapOf()
   private val anonymousTypes: MutableMap<QualifiedName, Type> = mutableMapOf()
   private val enumSynonymValues: MutableMap<EnumValueQualifiedName, CachedEnumSynonymValues> = mutableMapOf()

   init {
      timed("DefaultTypeCache initialisation") {
         types.forEach { add(it) }
      }
   }

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
      val withReference = type.copy(typeCache = this)
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
      return this.cache[name]
         ?: fromShortName(name)
         ?: parameterisedType(name)
         ?: anonymousTypes[name]
         ?: throw IllegalArgumentException("Type ${name.parameterizedName} was not found within this schema, and is not a valid short name")
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

   override fun enumSynonymsAsTypedValues(typedEnumValue: TypedEnumValue, valueKind: EnumValueKind): List<TypedValue> {
      return getEnumSynonyms(typedEnumValue).get(valueKind)
   }

   private fun getEnumSynonyms(typedEnumValue: TypedEnumValue): CachedEnumSynonymValues {
      return this.enumSynonymValues.getOrPut(typedEnumValue.enumValueQualifiedName) {

         val synonymTypedValues = typedEnumValue.enumValue.synonyms.map { synonymName ->
            val synonymQualifiedName = synonymName.synonymFullyQualifiedName()
            val synonymEnumValue = synonymName.synonymValue()
            val synonymEnumType = this.type(synonymQualifiedName)
            val synonymEnumTypedInstance = synonymEnumType.enumTypedInstance(synonymEnumValue)
            Triple(
               synonymEnumTypedInstance,
               synonymEnumTypedInstance.asTypedValue(EnumValueKind.NAME),
               synonymEnumTypedInstance.asTypedValue(EnumValueKind.VALUE)
            )
         }.toList()
         val synonymEnumValue = synonymTypedValues.map { it.first }
         val synonymValuesByName = synonymTypedValues.map { it.second }
         val synonymValuesByValue = synonymTypedValues.map { it.third }
         CachedEnumSynonymValues(synonymValuesByName, synonymValuesByValue,synonymEnumValue)
      }
   }

   override fun enumSynonyms(typedEnumValue: TypedEnumValue): List<TypedEnumValue> {
      return getEnumSynonyms(typedEnumValue).synonyms
   }

   private val isAssignableWithTypeParameters = mutableMapOf<String,Boolean>()
   private val isAssignableWithoutTypeParameters = mutableMapOf<String,Boolean>()
   override fun isAssignable(typeA: Type, typeB: Type, considerTypeParameters: Boolean, func:(Type,Type,Boolean) -> Boolean): Boolean {
      val key = typeA.fullyQualifiedName + "-[isAssignableTo]->" + typeB.fullyQualifiedName
      return if (considerTypeParameters) {
         isAssignableWithTypeParameters.getOrPut(key) { func(typeA,typeB,considerTypeParameters) }
      } else {
         isAssignableWithoutTypeParameters.getOrPut(key) { func(typeA,typeB,considerTypeParameters) }
      }
   }

   override fun hasType(name: String): Boolean {
      return shortNames[name]?.size == 1 || hasType(name.fqn())
   }
}
