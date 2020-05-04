package io.vyne.schemas

import io.vyne.VersionedSource

class SimpleSchema(override val types: Set<Type>, override val services: Set<Service>) : Schema {

   override val sources: List<VersionedSource> = emptyList()
   override val policies: Set<Policy> = emptySet()
   override val typeCache: TypeCache = DefaultTypeCache(this.types)
   override fun taxiType(name: QualifiedName): lang.taxi.types.Type {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
   }
}
data class DefaultTypeCache(private val types: Set<Type>) : TypeCache {
   private val cache: Map<QualifiedName, Type> = types.associateBy { it.name }
   private val shortNames: Map<String, Type>

   init {
      val possibleShortNames: MutableMap<String, Pair<Int, QualifiedName?>> = mutableMapOf()
      cache.forEach { name: QualifiedName, type ->
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

      if (hasType(name.fullyQualifiedName) && name.parameters.all { hasType(it) }) {
         // We've been asked for a parameterized type.
         // All the parameters are correctly defined, but no type exists.
         // This is caused by (for example), a service returning Array<Foo>, where both Array and Foo have been declared as types
         // but not Array<Foo> directly.
         // It's still valid, so we'll construct the type
         val baseType = type(name.fullyQualifiedName)
         val params = name.parameters.map { type(it) }
         return baseType.copy(name = name, typeParameters = params)
      } else {
         return null
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
