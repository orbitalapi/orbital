package io.vyne.schemas

import com.fasterxml.jackson.annotation.JsonIgnore

@Deprecated("This class fails to handle type extensions correctly.  Use TaxiSchema.fromNamedSources().first(), which will correctly order, compose and compile the sources")
class CompositeSchema(private val schemas: List<Schema>) : Schema {
   override val types: Set<Type>
      get() {
         return schemas.flatMap { it.types }.distinctBy { it.fullyQualifiedName }.toSet()
      }
   override val services: Set<Service>
      get() = schemas.flatMap { it.services }.distinctBy { it.qualifiedName }.toSet()

   override val policies: Set<Policy>
      @JsonIgnore
      get() = schemas.flatMap { it.policies }.distinctBy { it.name.fullyQualifiedName }.toSet()

   override val typeCache: TypeCache
      get() = DefaultTypeCache(this.types)
}
