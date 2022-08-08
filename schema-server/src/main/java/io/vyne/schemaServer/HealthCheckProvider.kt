package io.vyne.schemaServer

import io.vyne.schema.api.SchemaSourceProvider
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.ReactiveHealthIndicator
import reactor.core.publisher.Mono

class HealthCheckProvider(private val schemaProvider:SchemaSourceProvider) : ReactiveHealthIndicator {
   override fun health(): Mono<Health> {
      TODO("Not yet implemented")
   }
}
