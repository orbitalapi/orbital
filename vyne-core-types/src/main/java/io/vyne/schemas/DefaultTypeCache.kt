package io.vyne.schemas

import io.vyne.models.ConversionService
import io.vyne.models.DefinedInSchema
import io.vyne.models.TypedInstance
import io.vyne.models.TypedValue
import io.vyne.utils.timed
import lang.taxi.types.ObjectType
import java.util.function.BiFunction

class DefaultTypeCache(types: Set<Type> = emptySet()) : TypeCache {
   private val cache: MutableMap<QualifiedName, Type> = mutableMapOf()
   private val defaultValueCache: MutableMap<QualifiedName, Map<AttributeName, TypedInstance>?> = mutableMapOf()
   private var shortNames: MutableMap<String, MutableList<Type>> = mutableMapOf()
   private val anonymousTypes: MutableMap<QualifiedName, Type> = mutableMapOf()

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
         ?.map { field -> Pair(field.name,
            TypedValue.from(
               type = type(field.type.qualifiedName.fqn()),
               value = field.defaultValue!!,
               converter = ConversionService.DEFAULT_CONVERTER, source = DefinedInSchema)) }
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
            mutableListOf(type)
         } else {
            existingList.add(type)
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
         baseType.copy(name = name, typeParametersTypeNames = name.parameters)
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

   override fun hasType(name: String): Boolean {
      return shortNames[name]?.size == 1 || hasType(name.fqn())
   }
}
