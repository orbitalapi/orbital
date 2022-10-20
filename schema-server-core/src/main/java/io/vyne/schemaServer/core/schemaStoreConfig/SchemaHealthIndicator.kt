package io.vyne.schemaServer.core.schemaStoreConfig

import arrow.core.Either
import io.vyne.schemaStore.ValidatingSchemaStoreClient
import lang.taxi.errors
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.ReactiveHealthIndicator
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

/**
 * Reports schema file count and error count as health point stats.
 * Never actually reports the server as down, as having compilation errors is a valid state
 */
@Component
class SchemaHealthIndicator(
   private val schemaStoreClient: ValidatingSchemaStoreClient
) : ReactiveHealthIndicator {
   companion object {
      const val ERROR_COUNT = "errorCount"
      const val SOURCES_COUNT = "sourcesCount"
   }

   override fun health(): Mono<Health> {
      val lastSubmissionResult = schemaStoreClient.lastSubmissionResult
      val builder = Health.Builder()
         .up()
      when (lastSubmissionResult) {
         is Either.Left -> builder.withDetail(ERROR_COUNT, lastSubmissionResult.value.errors.errors().size)
         is Either.Right -> builder.withDetail(ERROR_COUNT, 0)
            .withDetail(SOURCES_COUNT, lastSubmissionResult.value.sources.size)
      }
      return Mono.just(builder.build())
   }
}
