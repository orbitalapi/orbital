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
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

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
      return doBuildMetadata(taxiQlQuery, schema)
      return cache.get(cacheKey.hashCode()) {
         doBuildMetadata(taxiQlQuery, schema)
      }
   }

   private fun doBuildMetadata(taxiQlQuery: TaxiQlQuery, schema: Schema): QueryPlanMetadata {
      // here, we avoid using the return type of the search if possible, as if the
      // query is a find-then-mutate, we want to build the query plan around the find phase, not the final mutation.
      // However, if there is no projection or source type, then this query is only a mutation, and it's safe the build the plan for.
      val searchType = taxiQlQuery.projectedType ?: taxiQlQuery.typesToFind.map { it.type }.singleOrNull() ?: taxiQlQuery.unwrappedReturnType
      val referencedTypes = collectReferencedTypes(searchType, schema) +
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

   /**
    * The set of the fewest operations required to attempt to serve this query.
    */
   private val minimumOperations: Set<Pair<Service, RemoteOperation>> = typesAndCandidateSources.let {
      // Calculate the fewest number of sources we need to contact in order
      // to complete this query, given that some attributes are available from multiple sources.
      // This is effectively a Set-Cover problem, which is NP-Hard.
      // This implementation follows the "Greedy" algorithm approach to solving.


      // 1: Invert the map
      val dataSourceToTypes = mutableMapOf<Pair<Service, RemoteOperation>, MutableSet<TypePresentInQuery>>()
      typesAndCandidateSources.forEach { (type, possibleSources) ->
         possibleSources.forEach { dataSource ->
            val typesServed = dataSourceToTypes.getOrPut(dataSource) { mutableSetOf() }
            typesServed.add(type)
         }
      }

      // 2: Build a set of the required types. We'll mutate this as we assign a data source
      // for each type
      val requiredTypes = typesAndCandidateSources.keys
         // Only consider types that have at least a single data source available
         .filter { a -> typesAndCandidateSources[a]?.isNotEmpty() ?: false }
         .toMutableSet()

      // Don't iterate forever
      val maximumPermittedLoops = requiredTypes.size * dataSourceToTypes.keys.size
      var loopCount = 0

      val selectedDataSources = mutableSetOf<Pair<Service, RemoteOperation>>()

      // Loop, finding the "best" data source for the remaining types.
      while (requiredTypes.isNotEmpty() && loopCount < maximumPermittedLoops) {
         // We select the data source that covers the most "yet-to-be-covered" types.
         val nextDataSource = dataSourceToTypes.maxByOrNull { it.value.count { type -> type in requiredTypes } }
         if (nextDataSource != null) {
            selectedDataSources.add(nextDataSource.key)
            requiredTypes.removeAll(nextDataSource.value)
         } else {
            break
         }
         loopCount++
      }
      if (requiredTypes.isNotEmpty()) {
         logger.warn { "Attempt to select minimum set of services for query failed - the following types were left unresolved (though do have data sources available) after the max iterations exceeded: ${requiredTypes.joinToString { it.type.name.shortDisplayName }}" }
      }

      selectedDataSources
   }

   val minimumStreamOperations = minimumOperations
      .filter { (_, operation) -> operation is StreamOperation || operation.returnType.isStream }


   val possibleStreamTypes = minimumStreamOperations.mapNotNull { it.second.returnType.typeParameters.firstOrNull() }
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


