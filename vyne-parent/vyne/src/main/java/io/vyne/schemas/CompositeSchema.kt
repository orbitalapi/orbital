package io.vyne.schemas

import com.fasterxml.jackson.annotation.JsonIgnore


class CompositeSchema(private val schemas: List<Schema>) : Schema {
   override val attributes: Set<QualifiedName>
      get() = schemas.flatMap { it.attributes }.toSet()
   override val types: Set<Type>
      get() = schemas.flatMap { it.types }.distinctBy { it.fullyQualifiedName }.toSet()
   override val links: Set<Link>
      get() = schemas.flatMap { it.links }.toSet()
   override val services: Set<Service>
      get() = schemas.flatMap { it.services }.distinctBy { it.qualifiedName }.toSet()

   override val policies: Set<Policy>
      @JsonIgnore
      get() = schemas.flatMap { it.policies }.distinctBy { it.name.fullyQualifiedName }.toSet()

   override val typeCache: TypeCache
      get() = DefaultTypeCache(this.types)
}
