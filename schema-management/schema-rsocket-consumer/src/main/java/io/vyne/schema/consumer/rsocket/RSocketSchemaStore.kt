package io.vyne.schema.consumer.rsocket

import io.vyne.schema.api.SchemaSet
import io.vyne.schema.consumer.SchemaSetChangedEventRepository
import io.vyne.schema.consumer.SchemaStore
import io.vyne.schema.rsocket.RSocketSchemaServerProxy
import mu.KotlinLogging
import java.util.concurrent.atomic.AtomicInteger

private val logger = KotlinLogging.logger { }


class RSocketSchemaStore(
   private val rSocketSchemaServerProxy: RSocketSchemaServerProxy
) : SchemaSetChangedEventRepository(), SchemaStore {
   private var schemaSet: SchemaSet = SchemaSet.EMPTY
   private val generationCounter: AtomicInteger = AtomicInteger(0)

   override fun schemaSet() = schemaSet

   override val generation: Int
      get() {
         return generationCounter.get()
      }

   init {
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
