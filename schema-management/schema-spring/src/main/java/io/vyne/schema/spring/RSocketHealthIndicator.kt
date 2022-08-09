package io.vyne.schema.spring

import io.vyne.schema.rsocket.SchemaServerRSocketFactory
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.ReactiveHealthIndicator
import reactor.core.publisher.Mono

/**
 * HealthIndicator for Spring Boot health endpoints.
 *
 * Shows the server as being DOWN if no schema server connection
 * is possible over RSocket
 */
class RSocketHealthIndicator(
   private val rSocketFactory: SchemaServerRSocketFactory
) : ReactiveHealthIndicator {
   override fun health(): Mono<Health> {
      return if (rSocketFactory.connectionEstablished) {
         Mono.just(Health.up().build())
      } else {
         Mono.just(Health.down().build())
      }
   }
}
