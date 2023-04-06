package io.vyne.query.runtime.core

import com.fasterxml.jackson.databind.ObjectMapper
import io.vyne.auth.tokens.AuthTokenRepository
import io.vyne.connectors.registry.RawConnectionsConnectorConfig
import io.vyne.http.ServicesConfigRepository
import io.vyne.query.ResultMode
import io.vyne.query.runtime.QueryMessage
import io.vyne.schema.api.SchemaProvider
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.body
import org.springframework.web.reactive.function.client.bodyToFlux
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Entry point for sending queries to
 * standalone query nodes.
 *
 * Used when this service is not responsible for query execution,
 * just for dispatching the query.
 */
@Component
@ConditionalOnProperty("vyne.query-router-url")
class QueryDispatcher(
   private val webClient: WebClient.Builder,
   private val servicesRepository: ServicesConfigRepository,
   private val authTokenRepository: AuthTokenRepository,
   private val connectionsConfigProvider: RawConnectionsConnectorConfig,
   private val schemaProvider: SchemaProvider,
   private val objectMapper: ObjectMapper,
   @Value("\${vyne.query-router-url}") private val queryRouterUrl: String
) {

   init {
      logger.info { "Query dispatcher is active - routing queries to $queryRouterUrl" }
   }

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   fun dispatchQuery(
      query: String,
      clientQueryId: String,
      mediaType: String,
      resultMode: ResultMode = ResultMode.RAW
   ): Flux<Any> {
      val message = QueryMessage(
         query = query,
         sourcePackages = schemaProvider.schema.packages,
         connections = connectionsConfigProvider.loadAsMap(),
         authTokens = authTokenRepository.getAllTokens(),
         services = servicesRepository.load(),
         resultMode, mediaType, clientQueryId
      )

      logger.info { "Received query $clientQueryId - $query" }

      return webClient.build().post()
         .uri(queryRouterUrl)
         .body(Mono.just(message))
         .exchangeToFlux { clientResponse ->
            clientResponse.bodyToFlux<Any>()
         }
   }

}
