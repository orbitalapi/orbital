package io.vyne.spring

import arrow.core.Either
import io.vyne.utils.log
import io.vyne.schemaStore.SchemaStoreClient
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener

class LocalSchemaPublisher(val schemaName: String,
                           val schemaVersion: String,
                           val localTaxiSchemaProvider: LocalTaxiSchemaProvider,
                           val schemaStoreClient: SchemaStoreClient) {
   private var startupPublishTriggered: Boolean = false

   @EventListener
   fun handleEvent(event: ContextRefreshedEvent) {
      if (!startupPublishTriggered) {
         startupPublishTriggered = true

         // Note: we need a try...catch here, because spring calls this eventHandler
         // via reflection, and seems to swallow exceptions
         try {
            publish()
         } catch (exception: Exception) {
            log().error("Failed to generate schema", exception)
            throw exception
         }

      }

   }

   fun publish() {
      // TODO : Add retry logic
      log().info("Publishing schemas")
      val schema = localTaxiSchemaProvider.schemaString()
      if (schema.isEmpty()) {
         log().error("No schemas found to publish")
      } else {
         log().debug("Attempting to register schema: $schema")
         val schemaValidationResult = schemaStoreClient.submitSchema(schemaName, schemaVersion, schema)
         when (schemaValidationResult) {
            is Either.Left -> log().error("Failed to register schema", schemaValidationResult.a.message)
            is Either.Right -> log().info("Schema registered successfully")
         }
      }
   }
}
