package io.vyne.schemas

import com.fasterxml.jackson.annotation.JsonIgnore
import io.vyne.VersionedSource
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.utils.log
import lang.taxi.Compiler
import lang.taxi.TaxiDocument

@Deprecated("This class fails to handle type extensions correctly.  Use TaxiSchema.fromNamedSources().first(), which will correctly order, compose and compile the sources")
class CompositeSchema(private val schemas: List<Schema>) : Schema {
   companion object {
      private val EMPTY_TAXI by lazy { Compiler("").compile() }
   }
   @get:JsonIgnore
   override val taxi: TaxiDocument
      get() {
         if (schemas.isEmpty()) {
            log().warn("No schemas are published")
            return EMPTY_TAXI
         }
         // TODO : Not keen on solving this problem, since CompositeSchemas are going away
         if (schemas.size != 1) {
            log().warn("Schema size should be 1 , but found ${schemas.size}")
            TODO()
         }
         return schemas.first().taxi
      }

   @get:JsonIgnore
   val taxiSchemas:List<TaxiSchema>
      get() {
         if (this.schemas.any { it !is TaxiSchema }) {
            log().error("CompositeSchema contains schemas that aren't TaxiSchema.  That's a problem, as we're migrating away from CompositeSchema")
         }
         return this.schemas.filterIsInstance<TaxiSchema>()
      }

   override val sources: List<VersionedSource>
      get() {
         return schemas.flatMap { it.sources }
      }
   override val types: Set<Type>
      get() {
         return schemas.flatMap { it.types }.distinctBy { it.fullyQualifiedName }.toSet()
      }
   override val services: Set<Service>
      get() = schemas.flatMap { it.services }.distinctBy { it.qualifiedName }.toSet()

   override val policies: Set<Policy>
      @JsonIgnore
      get() = schemas.flatMap { it.policies }.distinctBy { it.name.fullyQualifiedName }.toSet()

   override val typeCache: TypeCache by lazy {
      DefaultTypeCache(this.types)
   }

   override fun taxiType(name: QualifiedName): lang.taxi.types.Type {
      return schemas.first {
         it.hasType(name.fullyQualifiedName)
      }.taxiType(name)
   }
}
