package com.orbitalhq.schemas

import com.fasterxml.jackson.databind.ObjectMapper
import com.orbitalhq.annotations.streaming.StreamingQueryAnnotations
import com.orbitalhq.models.json.Jackson
import com.orbitalhq.query.caching.CacheAnnotation
import com.orbitalhq.query.caching.StateStore
import com.orbitalhq.query.caching.StateStoreAnnotation
import com.orbitalhq.query.caching.StateStoreConfig
import com.orbitalhq.serde.TaxiJacksonModule
import lang.taxi.annotations.HttpResponseHeader
import lang.taxi.query.Parameter
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
   fun cacheStrategy(query: TaxiQlQuery): CachingStrategy {
      val cacheAnnotation = query.annotation(CacheAnnotation.CacheTypeName.parameterizedName)
      return when {
         cacheAnnotation == null -> QueryScopedCache
         cacheAnnotation.parameter("connection") != null -> RemoteCache(cacheAnnotation.parameter("connection") as? String?)
         cacheAnnotation.defaultParameterValue != null -> NamedCache(cacheAnnotation.defaultParameterValue as String)
         cacheAnnotation.parameter("connection") == null -> RemoteCache(null)
         else -> GlobalSharedCache
      }
   }

   fun parseStateStoreConfig(query: TaxiQlQuery): Pair<Boolean, StateStoreConfig?> {
      val statStoreAnnotation = query.annotation(StateStoreAnnotation.StateStoreTypeName.parameterizedName)
      val useStateStore = statStoreAnnotation != null
      return Pair(useStateStore, statStoreAnnotation?.let { annotation ->
         val connectionName = annotation.parameter("connection") as? String
         val stateStoreName = annotation.parameter("name") as? String
         val maxIdleSeconds = annotation.parameter("maxIdleSeconds") as Int
         StateStoreConfig(connectionName, stateStoreName, maxIdleSeconds)
      })
   }

   fun streamConsumerId(query: TaxiQlQuery): String? {
      val streamConsumer = query.annotation(StreamingQueryAnnotations.StreamConsumerAnnotationName)
      return streamConsumer?.let { annotation ->
         annotation.parameter("id") as String?
      }
   }

   fun streamConsumerOffset(query: TaxiQlQuery): String? {
      val streamConsumer = query.annotation(StreamingQueryAnnotations.StreamConsumerAnnotationName)
      return streamConsumer?.let { annotation ->
         annotation.parameter("offset") as String?
      }
   }

   fun httpResponseParameters(taxiQlQuery: TaxiQlQuery): List<Parameter> {
      return taxiQlQuery.parameters.filter { parameter ->
         parameter.annotation(HttpResponseHeader.NAME) != null
      }
   }
}

data class QueryOptions(
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
   val stateStoreConfig: StateStoreConfig? = null,
   /**
    * Some queries require state (such as joining streams).
    * This indicates a connection to load from the SourceLoadersConnectionRegistry
    */
   val useStateStore: Boolean = false,

   /**
    * Streaming Queries might require an id which will be used to manage streaming subscriptions downstream (e.g. Setting the consumer group Ids for Kafka)
    */
   val streamConsumerId: String? = null,

   /**
    * Allows overriding the offset setting for streaming consumers (ie., Kafka), otherwise defaults to what's defined on
    * the operation's annotation
    */
   val streamConsumerOffset: String? = null,

   val responseHeaders: List<Parameter> = emptyList()
) {


   fun configure(mapper: ObjectMapper): ObjectMapper {
      return mapper.registerModule(TaxiJacksonModule)
   }

   fun newObjectMapper(): ObjectMapper {
      return configure(Jackson.newObjectMapperWithDefaults())
   }

   companion object {
      fun default() = QueryOptions()

      fun fromQuery(query: TaxiQlQuery): QueryOptions {
         val cachingStrategy: CachingStrategy = QueryOptionParameterKeys.cacheStrategy(query)
         val (useStateStore, stateStoreConfig) = QueryOptionParameterKeys.parseStateStoreConfig(query)
         val streamConsumerId = QueryOptionParameterKeys.streamConsumerId(query)
         val streamConsumerOffset = QueryOptionParameterKeys.streamConsumerOffset(query)
         val responseHeaders = QueryOptionParameterKeys.httpResponseParameters(query)
         return QueryOptions(
            cachingStrategy = cachingStrategy,
            stateStoreConfig = stateStoreConfig,
            useStateStore = useStateStore,
            streamConsumerId = streamConsumerId,
            streamConsumerOffset = streamConsumerOffset,
            responseHeaders = responseHeaders
         )
      }
   }
}
