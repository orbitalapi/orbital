package io.vyne.schemas

import io.vyne.VersionedSource
import io.vyne.schemas.taxi.TaxiSchema
import lang.taxi.TaxiDocument

class SimpleSchema(override val types: Set<Type>, override val services: Set<Service>, override val typeCache: TypeCache) : Schema {
   companion object {
      val EMPTY = SimpleSchema(emptySet(), emptySet(), DefaultTypeCache())
   }
   override val taxi: TaxiDocument
      get() = TODO("Not yet implemented")
   override val sources: List<VersionedSource> = emptyList()
   override val policies: Set<Policy> = emptySet()
   override fun hasType(name: String): Boolean {
      // Don't defer to base, which looks at the full typeCache.
      // We can use SimpleSchema to produce a subset of a schema.
      // In those instances, there's a difference between the types property
      // (ie., things we want to include in this schema subset)
      // and the types in the typeCache (ie., all the types we know about).
      // This *could* lead to compilation errors, but this SimpleSchema
      // implementation shouldn't really be used when you're trying to do stuff
      // that compiles.
      return types.any { it.fullyQualifiedName == name }
   }
   override fun taxiType(name: QualifiedName): lang.taxi.types.Type {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
   }

   override val dynamicMetadata: List<QualifiedName> = emptyList()
   override val metadataTypes: List<QualifiedName> = emptyList()

   override fun asTaxiSchema(): TaxiSchema {
      TODO("Not yet implemented")
   }
}
