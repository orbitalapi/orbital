package com.orbitalhq.query.runtime.core.dispatcher.http

import com.orbitalhq.auth.schemes.AuthSchemeRepository
import com.orbitalhq.connectors.config.SourceLoaderConnectorsRegistry
import com.orbitalhq.http.ServicesConfigRepository
import com.orbitalhq.query.ResultMode
import com.orbitalhq.query.runtime.CompressedQueryResultWrapper
import com.orbitalhq.query.runtime.QueryMessage
import com.orbitalhq.query.runtime.QueryMessageCborWrapper
import com.orbitalhq.query.runtime.core.dispatcher.StreamingQueryDispatcher
import com.orbitalhq.schema.api.SchemaProvider
import com.orbitalhq.utils.formatAsFileSize
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException.BadGateway
import org.springframework.web.reactive.function.client.body
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux

/**
 * Entry point for sending queries to
 * standalone query nodes.
 *
 * Used when this service is not responsible for query execution,
 * just for dispatching the query.
 */
@Component
@ConditionalOnProperty("vyne.dispatcher.http.enabled", havingValue = "true", matchIfMissing = false)
class HttpQueryDispatcher(
    private val webClient: WebClient.Builder,
    private val servicesRepository: ServicesConfigRepository,
    private val authTokenRepository: AuthSchemeRepository,
    private val connectionsConfigProvider: SourceLoaderConnectorsRegistry,
    private val schemaProvider: SchemaProvider,
    @Value("\${vyne.dispatcher.http.url}") private val queryRouterUrl: String
) : StreamingQueryDispatcher {

   init {
      logger.info { "Query dispatcher is active - routing queries to $queryRouterUrl" }
   }

   companion object {
      private val logger = KotlinLogging.logger {}
   }

   override fun dispatchQuery(
      query: String,
      clientQueryId: String,
      mediaType: String,
      resultMode: ResultMode,
      arguments: Map<String, Any?>
   ): Flux<Any> {
      val message = QueryMessage(
         query = query,
         sourcePackages = schemaProvider.schema.packages,
         connections = connectionsConfigProvider.load(),
         authTokens = authTokenRepository.getAllTokens(),
         services = servicesRepository.load(),
         resultMode, mediaType, clientQueryId,
         arguments
      )

      return dispatchQuery(message)
         .toFlux()
   }

//   override fun handleRoutedQuery(query: RoutedQuery): Flux<Any> {
//      return dispatchQuery(
//         query.querySrc,
//         Ids.fastUuid(),
//         MediaType.APPLICATION_JSON_VALUE,
//         arguments = query.argumentValues
//      )
//   }

   fun dispatchQuery(message: QueryMessage): Mono<Any> {
      val encodedWrapper = QueryMessageCborWrapper.from(message)
      logger.info { "Dispatching query ${message.clientQueryId} - ${encodedWrapper.size().formatAsFileSize}" }

      return webClient.build().post()
         .uri(queryRouterUrl)
         .body(Mono.just(encodedWrapper))
         .retrieve()
         .bodyToMono(CompressedQueryResultWrapper::class.java)
         .timed()
         .map { result ->
            logger.info { "Received result in ${result.elapsed()}- ${result.get().r.size.formatAsFileSize}" }
            result.get().decompressOrThrow()
         }
         .doOnError { error ->
            when (error) {
               is BadGateway -> handleBadGateway(error, message.clientQueryId)
               else -> logger.error(error) { "Query ${message.clientQueryId} failed: ${error.message}" }
            }

         }
   }

   private fun handleBadGateway(error: BadGateway, clientQueryId: String) {
      // Grab the tracing headers:
      val requestId = error.headers["x-amzn-RequestId"]?.joinToString() ?: "Not provided"
      val traceId = error.headers["X-Amzn-Trace-Id"]?.joinToString() ?: "Not provided"
      val errorBody = error.responseBodyAsString

      logger.warn { "Query $clientQueryId failed: ${error.message}. requestId = $requestId ; traceId = $traceId ; responseBody = $errorBody" }
   }


}
