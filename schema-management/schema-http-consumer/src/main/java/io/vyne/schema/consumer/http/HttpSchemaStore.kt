package io.vyne.schema.consumer.http

import io.vyne.schema.consumer.SchemaSetChangedEventRepository
import io.vyne.schema.consumer.SchemaStore
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import reactor.util.retry.Retry
import java.time.Duration

private val logger = KotlinLogging.logger {}

class HttpSchemaStore(
   private val httpListSchemasService: HttpListSchemasService,
   @Value("\${vyne.schema.pollInterval:5s}") private val pollInterval: Duration
) : SchemaSetChangedEventRepository(),
   SchemaStore {
   private val pollScheduler = Schedulers.newSingle("HttpSchemaStorePoller")

   init {
      logger.info("Initializing client vyne.schema.pollInterval=${pollInterval}")
   }

   @Volatile
   private var poll = true


   @PostConstruct
   fun startPolling() {
      try {
         logger.info { "Polling schemas from schema server" }
         Flux.interval(pollInterval)
            .doOnSubscribe { logger.info { "Polling Schemas every $pollInterval" } }
            .subscribeOn(pollScheduler)
            .concatMap { httpListSchemasService.listSchemas() }
            .retryWhen(Retry
               .indefinitely()
               .doBeforeRetry { retryError -> logger.warn { "Unexpected error in polling schemas ${retryError.failure()}" } }
            ).subscribe { retrievedSchemaSet ->
               emitNewSchemaIfDifferent(retrievedSchemaSet)
            }
      } catch (e: Exception) {
         logger.warn("Failed to fetch schemas: $e")
      }
   }

   @PreDestroy
   fun stopPolling() {
      poll = false
   }
}


