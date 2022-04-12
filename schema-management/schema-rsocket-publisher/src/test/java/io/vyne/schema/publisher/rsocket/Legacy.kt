package io.vyne.schema.publisher.rsocket

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.http.MediaType
import org.springframework.http.codec.cbor.Jackson2CborDecoder
import org.springframework.http.codec.cbor.Jackson2CborEncoder
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.RSocketStrategies
import org.springframework.web.util.pattern.PathPatternRouteMatcher
import reactor.core.publisher.Mono
import reactor.core.publisher.SignalType
import reactor.core.publisher.Sinks
import reactor.util.retry.Retry
import java.net.URI
import java.time.Duration

private val logger = KotlinLogging.logger {}

class Legacy {

   fun <T> schemaServerPublishSchemaConnection(
      setUpData: T,
      objectMapper: ObjectMapper
   ): Mono<RSocketRequester> {
      val setUpDataJsonStr = objectMapper.writeValueAsString(setUpData)
      return Mono.just("localhost" to 7655)
         .flatMap { (host, port) ->
            RSocketRequester
               .builder()
               .rsocketConnector { connector ->
                  connector.reconnect(
                     Retry.fixedDelay(Long.MAX_VALUE, Duration.ofSeconds(10))
                        .doBeforeRetry { retrySignal: Retry.RetrySignal? ->
                        }
                  )
               }
               .dataMimeType(MediaType.APPLICATION_CBOR)
               .rsocketStrategies(rsocketStrategies())
               .setupData(setUpDataJsonStr)
               .connectTcp(host, port)
               .map { requester ->
                  requester.rsocket().onClose().doFirst {
                  }.doOnError { error ->
                     logger.warn { "Schema Server Connection is DROPPED! $error" }
                  }.doFinally { signalType ->
                     logger.warn { "Schema Server Connection is DROPPED! ${signalType.name}" }
                  }
                     .subscribe()
                  requester
               }
         }
   }

   private fun rsocketStrategies() = RSocketStrategies.builder()
      .encoders { it.add(Jackson2CborEncoder()) }
      .decoders { it.add(Jackson2CborDecoder()) }
      .routeMatcher(PathPatternRouteMatcher())
      .build()


}
