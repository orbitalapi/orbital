package com.orbitalhq.query.runtime.core.gateway

import com.orbitalhq.errors.OrbitalQueryException
import com.orbitalhq.query.QueryFailedException
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.body
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration

/**
 * Utility object for handling deferred server responses with Flux streams.
 *
 * Tested via SavedQueryWithAuthPolicyIntegrationTest
 */
object DeferredServerResponsePublisher {
   private val logger = KotlinLogging.logger {}
   /**
    * Wraps a Flux of Any type and ensures that it emits at least one item
    * before sending a ServerResponse.
    *
    * This method addresses issues where ServerResponse needs to determine a status code
    * immediately, but the appropriate response (e.g., bad request) can only be
    * determined after the Flux has started emitting items.
    *
    * @param source The source Flux to be wrapped.
    * @return A Mono<ServerResponse> that will emit a response based on the source Flux.
    */
   fun wrapFlux(source: Flux<out Any>, responseHeaders: Map<String, List<String>>): Mono<ServerResponse> {
      return deferErrorUntilFirstResponse(source) { safeFlux ->
         // Return an OK response with the remaining flux as the body
         ServerResponse
            .ok()
            .headers {
               responseHeaders.forEach { responseHeader ->
                  responseHeader.value.forEach { responseHeaderValue ->
                     it.add(responseHeader.key, responseHeaderValue)
                  }
               }
            }
            .body(safeFlux)
      }
   }

   fun wrapEventStreamFlux(source: Flux<out Any>, responseHeaders: Map<String, List<String>>): Mono<ServerResponse> {
      return deferErrorUntilFirstResponse(source) { safeFlux ->
         val serverSentEvents = safeFlux.map { message ->
            ServerSentEvent.builder<Any>()
               .data(message)
               .build()
         }
         ServerResponse.ok()
            .contentType(MediaType.TEXT_EVENT_STREAM)
            .headers {
               responseHeaders.forEach { responseHeader ->
                  responseHeader.value.forEach { responseHeaderValue ->
                     it.add(responseHeader.key, responseHeaderValue)
                  }
               }
            }
            .body(BodyInserters.fromServerSentEvents(serverSentEvents))
      }
   }

   private fun deferErrorUntilFirstResponse(source: Flux<out Any>, builder: (Flux<Any>) -> Mono<ServerResponse>): Mono<ServerResponse> {
      // Convert the cold Flux to a hot Flux
      val hotFlux = source.publish().refCount(1, Duration.ofSeconds(2))

      return hotFlux.next()
         .flatMap { value ->
            // Concatenate the first value with the remaining flux
            val remainingFlux = Mono.just(value)
               .concatWith(hotFlux) as Flux<Any>

            builder(remainingFlux)
         }.onErrorResume { error -> handleError(error) }
   }


   private fun handleError(error: Throwable): Mono<out ServerResponse> =
      when {
         // Handle OrbitalQueryException with custom status
         error is OrbitalQueryException -> handleOrbitalQueryException(error)
         error is QueryFailedException && error.cause is OrbitalQueryException -> {
            handleOrbitalQueryException(error.cause as OrbitalQueryException)
         }
         // Handle other exceptions as internal server errors
         else -> {
            val message = error.message ?: "A ${error::class.simpleName} was thrown"
            logger.error(error) { message }
            ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR.value())
               .contentType(MediaType.TEXT_PLAIN)
               .bodyValue(error.message ?: "A ${error::class.simpleName} was thrown")
         }
      }

   private fun handleOrbitalQueryException(error: OrbitalQueryException): Mono<ServerResponse> {
      val (statusCode, errorBody, responseHeaders) = HttpErrorResponse.getErrorCodeAndPayload(error)
      return ServerResponse.status(statusCode)
         .headers { headersConsumer ->
            responseHeaders.forEach { (name, values) ->
               values.forEach { value ->
                  headersConsumer.add(name, value)
               }
            }
         }
         .bodyValue(errorBody)
   }

   fun wrapMono(mono: Mono<Any>, responseHeaders: Map<String, List<String>>): Mono<out ServerResponse> {
      return mono.flatMap { value ->
         ServerResponse
            .ok()
            .headers {
               responseHeaders.forEach { responseHeader ->
                  responseHeader.value.forEach { responseHeaderValue ->
                     it.add(responseHeader.key, responseHeaderValue)
                  }
               }
            }
            .bodyValue(value)
      }.onErrorResume { handleError(it) }
   }
}
