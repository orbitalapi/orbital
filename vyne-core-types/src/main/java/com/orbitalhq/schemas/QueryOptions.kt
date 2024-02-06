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

/**
 * Defines a remote cache to use. If the connection name isn't passed, then the default connection is used.
 */
data class RemoteCache(val connectionName: String?) : CachingStrategy()

object QueryOptionParameterKeys {
   const val Cache = "Cache"
   const val StateStore = "StateStore"
   const val StreamConsumer = "StreamConsumer"

   fun cacheStrategy(query: TaxiQlQuery): CachingStrategy {
      val cacheAnnotation = query.annotation(Cache)
     return when {
         cacheAnnotation == null -> QueryScopedCache
         cacheAnnotation.parameter("connection") != null -> RemoteCache(cacheAnnotation.parameter("connection") as? String?)
         cacheAnnotation.defaultParameterValue != null -> NamedCache(cacheAnnotation.defaultParameterValue as String)
         cacheAnnotation.parameter("connection") == null -> RemoteCache(null)
         else -> GlobalSharedCache
      }
   }

   fun stateStoreConnectionName(query: TaxiQlQuery): Pair<Boolean, String?>  {
      val statStoreAnnotation = query.annotation(StateStore)
      val useStateStore = statStoreAnnotation != null
     return Pair(useStateStore, statStoreAnnotation?.let { annotation ->
         when {
            annotation.parameter("connection") != null -> annotation.parameter("connection")!! as String
            else -> null
         }
      })
   }

   fun streamConsumerId(query: TaxiQlQuery): String? {
      val streamConsumer = query.annotation(StreamConsumer)
      return streamConsumer?.let { annotation ->
         when {
            annotation.parameter("id") != null -> annotation.parameter("id")!! as String
            else -> null
         }
      }
   }

}

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
   val cachingStrategy: CachingStrategy = QueryScopedCache,

   /**
    * Some queries require state (such as joining streams).
    * When useStateStore = true, we need a connection to load from the  SourceLoadersConnectionRegistry
    * which can be used to store state. (Typically a cache provider, such as Hazelcast or Redis)
    * Connection name can be null in which case we use the 'default' connection specified in the taxonomy.
    */
   val stateStoreConnectionName: String? = null,
   /**
    * Some queries require state (such as joining streams).
    * This indicates a connection to load from the SourceLoadersConnectionRegistry
    */
   val useStateStore: Boolean = false,

   /**
    * Streaming Queries might require an id which will be used to manage streaming subscriptions downstream (e.g. Setting the consumer group Ids for Kafka)
    */
   val streamConsumerId: String? = null
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
         val cachingStrategy: CachingStrategy = QueryOptionParameterKeys.cacheStrategy(query)
         val (useStateStore, stateStoreConnectionName) = QueryOptionParameterKeys.stateStoreConnectionName(query)
         val streamConsumerId = QueryOptionParameterKeys.streamConsumerId(query)
         return QueryOptions(
            omitNulls = query.annotation("OmitNulls") != null,
            cachingStrategy = cachingStrategy,
            stateStoreConnectionName = stateStoreConnectionName,
            useStateStore = useStateStore,
            streamConsumerId = streamConsumerId
         )
      }
   }
}
