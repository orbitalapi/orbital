package io.vyne.schemas

import io.vyne.VersionedSource
import lang.taxi.Equality
import lang.taxi.TaxiDocument

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
   private var shortNames: Map<String, Type> = emptyMap()

   init {
      types.forEach { add(it) }
      recalculateShortNames()
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

   /**
    * Adds the type to the cache, and returns a new copy, with the
    * type cache updated.
    */
   fun add(type: Type): Type {
      val withReference = type.copy(typeCache = this)
      cache[type.name] = withReference
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
         ?: throw IllegalArgumentException("Type ${name.parameterizedName} was not found within this schema, and is not a valid short name")
//      }

//      return type(name.fullyQualifiedName)
   }

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
      if (name.parameters.isNotEmpty()) {
         return hasType(name.fullyQualifiedName) // this is the base type
            && name.parameters.all { hasType(it) }
      }
      return false
   }

   override fun hasType(name: String): Boolean {
      return shortNames.containsKey(name) || hasType(name.fqn())
   }
}
