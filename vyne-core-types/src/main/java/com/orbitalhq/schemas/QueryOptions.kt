package com.orbitalhq.schemas

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.orbitalhq.models.json.Jackson
import lang.taxi.query.TaxiQlQuery
import lang.taxi.types.annotation

/**
 * Defines how operations invoked by a query
 * are cached.
 */
sealed class CachingStrategy

/**
 * Cached throughout the query only
 */
object QueryScopedCache : CachingStrategy()

/**
 * A shared global cache which is used by all queries
 * that opt-in to long-lived caching, but don't specify
 * a named cache.
 */
object GlobalSharedCache : CachingStrategy()

/**
 * Defines a specific, named cache that is long-lived.
 * Eg: A saved query may choose to have it's own cache, which
 * is not shared with other saved queries
 */
data class NamedCache(val name: String) : CachingStrategy()

data class RemoteCache(val connectionName: String) : CachingStrategy()

data class QueryOptions(
   /**
    * indciates that fields which are null
    * should not be serialized in the result.
    *
    * As a result, reponses may not satisfy the query contract
    * which expects fields to be present, and consumers should apply
    * leineint parsing.
    */
   val omitNulls: Boolean = false,

   /**
    * Indicates that the query should leverage the global cache for operation invocations.
    * The default (false) uses a query-scoped cache, which is discarded at the end of the
    * query.
    *
    * Enabling this uses a global cache, which is shared between queries
    */
   val cachingStrategy: CachingStrategy = QueryScopedCache
) {

   /**
    * Indicates if these query options mean a custom mapper
    * should be used.
    */
   val requiresCustomMapper = omitNulls
   fun configure(mapper: ObjectMapper): ObjectMapper {
      if (omitNulls) {
         mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
      }
      return mapper
   }

   fun newObjectMapper(): ObjectMapper {
      return configure(Jackson.newObjectMapperWithDefaults())
   }

   fun newObjectMapperIfRequired(): ObjectMapper? {
      return if (requiresCustomMapper) {
         newObjectMapper()
      } else {
         null
      }
   }

   companion object {
      fun default() = QueryOptions()

      fun fromQuery(query: TaxiQlQuery): QueryOptions {
         val cachingStrategy: CachingStrategy = query.annotation("Cache")?.let { annotation ->
            when {
               annotation.parameter("connection") != null -> RemoteCache(annotation.parameter("connection")!! as String)
               annotation.defaultParameterValue != null -> NamedCache(annotation.defaultParameterValue as String)
               else -> GlobalSharedCache
            }
         } ?: QueryScopedCache
         return QueryOptions(
            omitNulls = query.annotation("OmitNulls") != null,
            cachingStrategy = cachingStrategy
         )
      }
   }
}
