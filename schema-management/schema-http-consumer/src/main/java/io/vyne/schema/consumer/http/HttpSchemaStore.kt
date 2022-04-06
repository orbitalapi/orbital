package io.vyne.schema.consumer.http

import io.vyne.schema.api.SchemaSet
import io.vyne.schema.consumer.SchemaSetChangedEventRepository
import io.vyne.schema.consumer.SchemaStore
import io.vyne.schemas.SchemaSetChangedEvent
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import reactor.util.retry.Retry
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

private val logger = KotlinLogging.logger {}


class HttpSchemaStore(
   private val httpListSchemasService: HttpListSchemasService,
   @Value("\${vyne.schema.pollInterval:5s}") private val pollInterval: Duration) : SchemaSetChangedEventRepository(),
   SchemaStore {
   private val pollScheduler = Schedulers.newSingle("HttpSchemaStorePoller")
   init {
      logger.info("Initializing client vyne.schema.pollInterval=${pollInterval}")
   }

   @Volatile
   private var poll = true
   private var schemaSet: SchemaSet = SchemaSet.EMPTY
   private val generationCounter: AtomicInteger = AtomicInteger(0)

   override fun schemaSet() = schemaSet

   override val generation: Int
      get() {
         return generationCounter.get()
      }


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
            ).subscribe  { retrievedSchemaSet ->
               retrievedSchemaSet?.let { receivedSchemaSet ->
                  this.publishSchemaSetChangedEvent(this.schemaSet, receivedSchemaSet) { publishedSchemaSet ->
                     this.schemaSet = publishedSchemaSet
                     generationCounter.incrementAndGet()
                     logger.info("Updated to SchemaSet ${schemaSet.id}, generation $generation, ${schemaSet.size()} schemas, ${schemaSet.sources.map { it.source.id }}")
                  }
               }
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


