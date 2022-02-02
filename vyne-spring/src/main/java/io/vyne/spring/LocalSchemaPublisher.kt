package io.vyne.spring

import arrow.core.Either
import io.vyne.schemaApi.SchemaSourceProvider
import io.vyne.schemaPublisherApi.SchemaPublisher
import io.vyne.utils.log
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import kotlin.concurrent.thread

class LocalSchemaPublisher(val schemaName: String,
                           val schemaVersion: String,
                           val localTaxiSchemaProvider: SchemaSourceProvider,
                           val schemaPublisher: SchemaPublisher) {
   private var startupPublishTriggered: Boolean = false

   @EventListener
   fun handleEvent(event: ContextRefreshedEvent) {
      if (!startupPublishTriggered) {
         startupPublishTriggered = true
         if (event.applicationContext.environment.getProperty("vyne.schme.publish.on.samethread") != null) {
            try {
               log().info("Publishing the schema on calling thread")
               publish()
            } catch (exception: Exception) {
               log().error("Failed to generate schema", exception)
               throw exception
            }
         } else {
            thread(start = true) {
               log().info("Context refreshed, triggering schema publication")
               try {
                  publish()
               } catch (exception: Exception) {
                  log().error("Failed to generate schema", exception)
                  throw exception
               }
            }
         }
      }
   }

   fun publish() {
      // HttpSchemaStoreClient has a built-in retry logic, TODO - add to others.
      log().info("Publishing schemas")
      val schema = localTaxiSchemaProvider.schemaString()
      if (schema.isEmpty()) {
         log().error("No schemas found to publish")
      } else {
         log().debug("Attempting to register schema: $schema")
         val schemaValidationResult = schemaPublisher.submitSchema(schemaName, schemaVersion, schema)
         when (schemaValidationResult) {
            is Either.Left -> log().error("Failed to register schema: ${schemaValidationResult.a.message}")
            is Either.Right -> log().info("Schema registered successfully")
         }
      }
   }
}
