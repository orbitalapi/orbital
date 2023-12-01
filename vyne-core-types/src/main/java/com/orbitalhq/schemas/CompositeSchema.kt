package com.orbitalhq.schemas

import com.fasterxml.jackson.annotation.JsonIgnore
import com.orbitalhq.PathGlob
import com.orbitalhq.SourcePackage
import com.orbitalhq.VersionedSource
import com.orbitalhq.schemas.taxi.TaxiSchema
import lang.taxi.TaxiDocument
import lang.taxi.packages.SourcesType
import lang.taxi.query.TaxiQLQueryString
import lang.taxi.query.TaxiQlQuery

@Deprecated("This class fails to handle type extensions correctly.  Use TaxiSchema.fromNamedSources().first(), which will correctly order, compose and compile the sources")
class CompositeSchema(private val schemas: List<Schema>) : Schema {
   private val queryCompiler = DefaultQueryCompiler(this, 100)
   override fun parseQuery(vyneQlQuery: TaxiQLQueryString): Pair<TaxiQlQuery, QueryOptions> {
      return queryCompiler.compile(vyneQlQuery)
   }

   @get:JsonIgnore
   override val taxi: TaxiDocument = when {
      schemas.isEmpty() -> TaxiDocument.empty()
      schemas.size == 1 -> schemas.first().taxi
      else -> error("Schema size should be  at most 1 ${schemas.size}")
   }

   @get:JsonIgnore
   override val additionalSourcePaths: List<Pair<String, PathGlob>> = this.schemas.flatMap { it.additionalSourcePaths }

   override val additionalSources: Map<SourcesType, List<SourcePackage>>
      get() {
         // Merge the maps
         val allAdditionalSources = this.schemas.map { it.additionalSources }
         if (allAdditionalSources.isEmpty()) {
            return emptyMap()
         }
         val merged: Map<SourcesType, List<SourcePackage>> = allAdditionalSources
            .reduce { acc, thisValue ->
               thisValue.entries.associate { (key, values) ->
                  val existingSources = acc[key]?.toMutableList() ?: mutableListOf()
                  existingSources.addAll(values)
                  key to existingSources
               }
            }
         return merged
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
   override val services: Set<Service> = schemas.flatMap { it.services }.distinctBy { it.fullyQualifiedName }.toSet()
   override val queries: Set<SavedQuery> =
      schemas.flatMap { it.queries }.distinctBy { it.name.fullyQualifiedName }.toSet()

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
