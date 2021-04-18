package io.vyne.schemas

import io.vyne.VersionedSource
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
