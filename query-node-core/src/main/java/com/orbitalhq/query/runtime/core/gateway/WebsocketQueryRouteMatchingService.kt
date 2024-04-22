package com.orbitalhq.query.runtime.core.gateway

import com.orbitalhq.schema.api.SchemaSet
import com.orbitalhq.schema.consumer.SchemaStore
import lang.taxi.query.TaxiQlQuery
import mu.KotlinLogging
import org.springframework.stereotype.Component
import reactor.kotlin.core.publisher.toFlux

/**
 * Provides websocket equivalent of QueryRouteService
 * for stream based queries.
 */
@Component
class WebsocketQueryRouteMatchingService(
   private val schemaStore: SchemaStore,
) {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   private var websocketQueries: Map<String, TaxiQlQuery> = emptyMap()

   init {
      schemaStore.schemaChanged
         .toFlux()
         .subscribe { event -> buildQueryIndex(event.newSchemaSet) }
      buildQueryIndex(schemaStore.schemaSet)
   }

   private fun buildQueryIndex(schemaSet: SchemaSet) {
      logger.info { "Schema changed, rebuilding stream handlers for schema generation ${schemaSet.generation}" }
      websocketQueries = QueryRouter.forWebsocketQueries(schemaSet.schema.taxi.queries)
      logger.info {
         "Router updated, now contains the following routes for streams: \n${
            websocketQueries.map { "${it.key} -> ${it.value.name.parameterizedName}" }.joinToString("\n")
         }"
      }
   }
   fun getQuery(route:String):TaxiQlQuery? = websocketQueries[route]
}
