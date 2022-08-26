package io.vyne.schemas

import com.fasterxml.jackson.annotation.JsonIgnore
import io.vyne.SourcePackage
import io.vyne.VersionedSource
import io.vyne.schemas.taxi.TaxiSchema
import lang.taxi.TaxiDocument

@Deprecated("This class fails to handle type extensions correctly.  Use TaxiSchema.fromNamedSources().first(), which will correctly order, compose and compile the sources")
class CompositeSchema(private val schemas: List<Schema>) : Schema {

   @get:JsonIgnore
   override val taxi: TaxiDocument = when {
      schemas.isEmpty() -> TaxiDocument.empty()
      schemas.size == 1 -> schemas.first().taxi
      else -> error("Schema size should be  at most 1 ${schemas.size}")
   }

   @get:JsonIgnore
   override val packages: List<SourcePackage> = this.schemas.flatMap { it.packages }

   @get:JsonIgnore
   val taxiSchemas: List<TaxiSchema> = this.schemas.filterIsInstance<TaxiSchema>()

   override fun asTaxiSchema(): TaxiSchema {
      return when {
         this.taxiSchemas.isEmpty() -> TaxiSchema.empty()
         this.taxiSchemas.size == 1 -> this.taxiSchemas.single()
         else -> error("Expected exactly one taxi schema.  Composite schemas with multiple taxi schemas aren't supported anymore - compose before passing to the Composite schema")
      }
   }

   override val sources: List<VersionedSource> = schemas.flatMap { it.sources }
   override val types: Set<Type> = schemas.flatMap { it.types }.distinctBy { it.fullyQualifiedName }.toSet()
   override val services: Set<Service> = schemas.flatMap { it.services }.distinctBy { it.qualifiedName }.toSet()
   override val dynamicMetadata: List<QualifiedName> = schemas.flatMap { it.dynamicMetadata }.distinct()
   override val metadataTypes: List<QualifiedName> = schemas.flatMap { it.metadataTypes }.distinct()

   @JsonIgnore
   override val policies: Set<Policy> =
      schemas.flatMap { it.policies }.distinctBy { it.name.fullyQualifiedName }.toSet()

   override val typeCache: TypeCache = DefaultTypeCache(this.types)

   override fun taxiType(name: QualifiedName): lang.taxi.types.Type {
      return schemas.first {
         it.hasType(name.fullyQualifiedName)
      }.taxiType(name)
   }
}
