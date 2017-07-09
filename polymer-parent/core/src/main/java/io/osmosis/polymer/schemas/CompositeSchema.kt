package io.osmosis.polymer.schemas

class CompositeSchema(val schemas:List<Schema>) : Schema {
   override val attributes: Set<QualifiedName>
      get() = schemas.flatMap { it.attributes }.toSet()
   override val types: Set<Type>
      get() = schemas.flatMap { it.types }.toSet()
   override val links: Set<Link>
      get() = schemas.flatMap { it.links }.toSet()

}
