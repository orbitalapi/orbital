package io.vyne.rSocketSchemaConsumer

import io.vyne.schemaApi.SchemaSet
import io.vyne.schemaConsumerApi.SchemaSetChangedEventRepository
import io.vyne.schemaConsumerApi.SchemaStore
import io.vyne.schemas.SchemaSetChangedEvent
import io.vyne.schemeRSocketCommon.RSocketSchemaServerProxy
import mu.KotlinLogging
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.ApplicationEventPublisher
import java.util.concurrent.atomic.AtomicInteger

private val logger = KotlinLogging.logger {  }


class RSocketSchemaStore(
   private val eventPublisher: ApplicationEventPublisher,
   private val rSocketSchemaServerProxy: RSocketSchemaServerProxy): SchemaSetChangedEventRepository(), SchemaStore, InitializingBean {
   private var schemaSet: SchemaSet = SchemaSet.EMPTY
   private val generationCounter: AtomicInteger = AtomicInteger(0)

   override fun schemaSet() = schemaSet

   override val generation: Int
      get() {
         return generationCounter.get()
      }

   override fun afterPropertiesSet() {
      rSocketSchemaServerProxy.consumeSchemaSets { newSchemaSet ->
         this.publishSchemaSetChangedEvent(schemaSet, newSchemaSet) { this.onSchemaSetUpdate(it) }
      }
   }

   private fun onSchemaSetUpdate(receivedSchemaSet: SchemaSet) {
      schemaSet = receivedSchemaSet
      generationCounter.incrementAndGet()
      logger.info("Updated to SchemaSet ${schemaSet.id}, generation $generation, ${schemaSet.size()} schemas, ${schemaSet.sources.map { it.source.id }}")
   }
}
