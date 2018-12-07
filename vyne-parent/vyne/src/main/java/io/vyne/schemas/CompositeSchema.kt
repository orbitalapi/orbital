package io.vyne.schemas


class CompositeSchema(private val schemas: List<Schema>) : Schema {
   override val attributes: Set<QualifiedName>
      get() = schemas.flatMap { it.attributes }.toSet()
   override val types: Set<Type>
      get() = schemas.flatMap { it.types }.toSet()
   override val links: Set<Link>
      get() = schemas.flatMap { it.links }.toSet()
   override val services: Set<Service>
      get() = schemas.flatMap { it.services }.toSet()

   override val policies: Set<Policy>
      get() = schemas.flatMap { it.policies }.toSet()

   override val typeCache: TypeCache
      get() = DefaultTypeCache(this.types)
}
