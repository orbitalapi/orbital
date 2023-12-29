package com.orbitalhq.query.planner

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.orbitalhq.schemas.RemoteOperation
import com.orbitalhq.schemas.Schema
import com.orbitalhq.schemas.Service
import com.orbitalhq.schemas.StreamOperation
import com.orbitalhq.schemas.Type
import com.orbitalhq.schemas.taxi.toVyneType
import lang.taxi.query.TaxiQlQuery
import lang.taxi.types.ArrayType
import lang.taxi.types.Field
import lang.taxi.types.ObjectType

/**
 * Constructs metadata about the query, useful for planning and query optimizing
 */
class QueryMetadataBuilder(cacheSize: Int = 50) {

   private val cache: Cache<QueryPlanCacheKeyHashCode, QueryPlanMetadata> = CacheBuilder.newBuilder()
      .maximumSize(cacheSize.toLong())
      .build()

   /**
    * Creates query metadata, serving from the cache if available.
    */
   fun buildMetadata(taxiQlQuery: TaxiQlQuery, schema: Schema): QueryPlanMetadata {
      val cacheKey = QueryPlanCacheKey(taxiQlQuery, schema)
      return cache.get(cacheKey.hashCode()) {
         doBuildMetadata(taxiQlQuery, schema)
      }
   }

   private fun doBuildMetadata(taxiQlQuery: TaxiQlQuery, schema: Schema): QueryPlanMetadata {
      val referencedTypes = collectReferencedTypes(taxiQlQuery.unwrappedReturnType, schema) +
         taxiQlQuery.typesToFind.map { TopLevelType(it.type.toVyneType(schema)) }
      val providedTypes = taxiQlQuery.facts.map { it.type }.toSet()
      val typesAndCandidateOperations = referencedTypes
         .filter { !providedTypes.contains(it.type.taxiType) }
         .associateWith { typePresentInQuery ->
            getOperationsProviding(typePresentInQuery, schema)
         }
      return QueryPlanMetadata(
         taxiQlQuery,
         typesAndCandidateOperations,
         schema
      )
   }

   private fun getOperationsProviding(
      typePresentInQuery: TypePresentInQuery,
      schema: Schema
   ): Set<Pair<Service, RemoteOperation>> {
      return schema.operationsWithReturnTypeContaining(
         typePresentInQuery.unwrappedType,
      ) + schema.operationsWithReturnType(typePresentInQuery.type)
   }

   private fun collectReferencedTypes(
      type: lang.taxi.types.Type,
      schema: Schema,
      path: String = "$",
      visitedTypes: MutableSet<lang.taxi.types.Type> = mutableSetOf()
   ): List<TypePresentInQuery> {
      return if (visitedTypes.contains(type)) {
         emptyList()
      } else when (type) {
         is ObjectType -> collectReferencedTypes(type, schema, path, visitedTypes)
         is ArrayType -> collectReferencedTypes(type.memberType, schema, path, visitedTypes)
         else -> emptyList()
      }
   }

   private fun collectReferencedTypes(
      type: ObjectType,
      schema: Schema,
      path: String,
      visitedTypes: MutableSet<lang.taxi.types.Type>
   ): List<TypePresentInQuery> {
      visitedTypes.add(type)

      return type.allFields
         .filter { field -> field.accessor == null }
         .flatMap { field ->
            val fieldPath = "$path.${field.name}"
            val fieldProjectionSourceType = field.projection?.sourceType?.let { fieldProjectionSourceType ->
               FieldAndPath(
                  fieldPath,
                  field,
                  schema,
                  fieldProjectionSourceType.toVyneType(schema)
               )
            }
            listOfNotNull(
               FieldAndPath(fieldPath, field, schema),
               fieldProjectionSourceType
            ) + collectReferencedTypes(
               field.type,
               schema,
               fieldPath,
               visitedTypes
            )
         }
   }
}

/**
 * A collection of metadata and attributes about the query that can be used to
 * build a query plan.
 */
data class QueryPlanMetadata(
   val query: TaxiQlQuery,

   /**
    * A multimap of all fields (flattened) in the query,
    * along with a set of operations that could return the value.
    *
    * The query execution may not use these set of operations,
    * as values could be sourced elsewhere (Eg., provided as constants in the query).
    */
   val typesAndCandidateSources: Map<TypePresentInQuery, Collection<Pair<Service, RemoteOperation>>>,

   /**
    * The schema the query plan was compiled against.
    * Should contain any anonymous types present in the query.
    */
   val schema: Schema,
) {
   val allCandidateOperations = typesAndCandidateSources.values.flatten().distinctBy { it.second.qualifiedName }
   val candidateStreamOperations = allCandidateOperations
      .filter { (_, operation) -> operation is StreamOperation || operation.returnType.isStream}

   val possibleStreamTypes = candidateStreamOperations.mapNotNull { it.second.returnType.typeParameters.firstOrNull() }
}


/**
 * A type that has been referenced in a query - either in the response, or as a starting point
 */
sealed interface TypePresentInQuery {
   val type: Type

   /**
    * If type is an array or stream, returns the inner type
    */
   val unwrappedType: Type
      get() {
         return if (type.typeParameters.isNotEmpty()) {
            type.typeParameters[0]
         } else type
      }
}

data class FieldAndPath(
   val path: String,
   val field: Field,
   val schema: Schema,
   override val type: Type = schema.type(field.type)
) :
   TypePresentInQuery

data class TopLevelType(override val type: Type) : TypePresentInQuery


