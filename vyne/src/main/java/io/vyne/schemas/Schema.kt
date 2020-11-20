package io.vyne.schemas

import io.vyne.VersionedSource
import io.vyne.models.ConversionService
import io.vyne.models.DefinedInSchema
import io.vyne.models.TypedInstance
import io.vyne.models.TypedValue
import lang.taxi.TaxiDocument
import lang.taxi.types.ObjectType
import lang.taxi.utils.log

class SimpleSchema(override val types: Set<Type>, override val services: Set<Service>) : Schema {

   override val taxi: TaxiDocument
      get() = TODO("Not yet implemented")
   override val sources: List<VersionedSource> = emptyList()
   override val policies: Set<Policy> = emptySet()
   override val typeCache: TypeCache = DefaultTypeCache(this.types)
   override fun taxiType(name: QualifiedName): lang.taxi.types.Type {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
   }
}

class DefaultTypeCache(types: Set<Type> = emptySet()) : TypeCache {
   private val cache: MutableMap<QualifiedName, Type> = mutableMapOf()
   private val defaultValueCache: MutableMap<QualifiedName, Map<AttributeName, TypedInstance>?> = mutableMapOf()
   private var shortNames: Map<String, Type> = emptyMap()
   private val anonymousTypes: MutableMap<QualifiedName, Type> = mutableMapOf()

   init {
      log().info("DefaultTypeCache initialized")
      types.forEach { add(it) }
      recalculateShortNames()
      populateDefaultValuesCache()
   }

   val types: Set<Type>
      get() {
         return this.cache.values.toSet()
      }

   private fun recalculateShortNames() {
      val possibleShortNames: MutableMap<String, Pair<Int, QualifiedName?>> = mutableMapOf()
      cache.forEach { (name: QualifiedName, type) ->
         possibleShortNames.compute(name.name) { _, existingPair ->
            if (existingPair == null) {
               1 to type.name
            } else {
               existingPair.first + 1 to null
            }
         }
      }
      shortNames = possibleShortNames
         .filter { (shortName, countAndFqn) -> countAndFqn.first == 1 }
         .map { (shortName, countAndFqn) ->
            val type = this.cache[countAndFqn.second!!] ?: error("Expected a type named ${countAndFqn.second!!}")
            shortName to type
         }.toMap()
   }

   fun populateDefaultValuesCache() {
      cache.forEach {  (name: QualifiedName, type) ->
         defaultValueCache[name] = (type.taxiType as? ObjectType)
            ?.fields
            ?.filter { field -> field.defaultValue != null }
            ?.map { field -> Pair(field.name,
               TypedValue.from(
                  type = type(field.type.qualifiedName.fqn()),
                  value = field.defaultValue!!,
                  converter = ConversionService.DEFAULT_CONVERTER, source = DefinedInSchema)) }
            ?.toMap()
      }
   }

   /**
    * Adds the type to the cache, and returns a new copy, with the
    * type cache updated.
    */
   fun add(type: Type): Type {
      val withReference = type.copy(typeCache = this)
      cache[type.name] = withReference
      // TODO caching for parametrised types e.g. lang.taxi.Array<Order> (for each Type)
      recalculateShortNames()
      return withReference
   }

   override fun type(name: String): Type {
      return type(name.fqn())
   }

   override fun type(name: QualifiedName): Type {
//      if (isArrayType(name)) {
//         val typeNameWithoutArray = name.substringBeforeLast("[]")
//         val type = type(typeNameWithoutArray)
//         TODO()
//      } else {
      return this.cache[name]
         ?: this.shortNames[name.fullyQualifiedName]
         ?: parameterisedType(name)
         ?: fromDynamicTypes(name)
         ?: throw IllegalArgumentException("Type ${name.parameterizedName} was not found within this schema, and is not a valid short name")
//      }

//      return type(name.fullyQualifiedName)
   }

   private fun fromDynamicTypes(name: QualifiedName): Type? = anonymousTypes[name]

   // TODO implement some caching
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
      this.anonymousTypes[anonymousType.qualifiedName] = anonymousType
   }

   override fun anonymousTypes(): Set<Type> {
      return this.anonymousTypes.values.toSet()
   }

   override fun hasType(name: String): Boolean {
      return shortNames.containsKey(name) || hasType(name.fqn())
   }
}
