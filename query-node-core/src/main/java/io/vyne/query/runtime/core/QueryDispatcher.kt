package io.vyne.query.runtime.core

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.vyne.auth.tokens.AuthTokenRepository
import io.vyne.connectors.config.ConfigFileConnectorsRegistry
import io.vyne.http.ServicesConfigRepository
import io.vyne.query.ResultMode
import io.vyne.query.runtime.CompressedQueryResultWrapper
import io.vyne.query.runtime.QueryMessage
import io.vyne.query.runtime.QueryMessageCborWrapper
import io.vyne.schema.api.SchemaProvider
import io.vyne.utils.formatAsFileSize
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.encodeToStream
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.util.function.Tuple2
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

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
   private val connectionsConfigProvider: ConfigFileConnectorsRegistry,
   private val schemaProvider: SchemaProvider,
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
   ): Mono<Any> {
      val message = QueryMessage(
         query = query,
         sourcePackages = schemaProvider.schema.packages,
         connections = connectionsConfigProvider.load(),
         authTokens = authTokenRepository.getAllTokens(),
         services = servicesRepository.load(),
         resultMode, mediaType, clientQueryId
      )

      val encodedWrapper = QueryMessageCborWrapper.from(message)
      logger.info { "Dispatching query $clientQueryId - ${encodedWrapper.size().formatAsFileSize}" }

      return webClient.build().post()
         .uri(queryRouterUrl)
         .body(Mono.just(encodedWrapper))
         .retrieve()
         .bodyToMono(CompressedQueryResultWrapper::class.java)
         .timed()
         .map { result ->
            logger.info { "Received result in ${result.elapsed()}- ${result.get().r.size.formatAsFileSize}" }
            result.get().decompress()
         }
   }

}
