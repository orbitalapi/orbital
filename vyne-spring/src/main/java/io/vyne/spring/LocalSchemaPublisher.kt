package io.vyne.spring

import arrow.core.Either
import io.vyne.schema.api.SchemaSourceProvider
import io.vyne.schema.publisher.SchemaPublisherTransport
import mu.KotlinLogging
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import kotlin.concurrent.thread

private val logger = KotlinLogging.logger { }

// How his this used?
// Don't think it is...
//class LocalSchemaPublisher(
//   val schemaName: String,
//   private val schemaVersion: String,
//   val localTaxiSchemaProvider: SchemaSourceProvider,
//   val schemaPublisher: SchemaPublisherTransport
//) {
//   private var startupPublishTriggered: Boolean = false
//
//   init {
//      error("Document what this is used for, or delete it.")
//   }
//
//   @EventListener
//   fun handleEvent(event: ContextRefreshedEvent) {
//      if (!startupPublishTriggered) {
//         startupPublishTriggered = true
//         if (event.applicationContext.environment.getProperty("vyne.schme.publish.on.samethread") != null) {
//            try {
//               logger.info("Publishing the schema on calling thread")
//               publish()
//            } catch (exception: Exception) {
//               logger.error("Failed to generate schema", exception)
//               throw exception
//            }
//         } else {
//            thread(start = true) {
//               logger.info("Context refreshed, triggering schema publication")
//               try {
//                  publish()
//               } catch (exception: Exception) {
//                  logger.error(exception) { "Failed to generate schema" }
//                  throw exception
//               }
//            }
//         }
//      }
//   }
//
//   fun publish() {
//      if (localTaxiSchemaProvider is SchemaSourceProvider) {
//         publishVersionedSources(localTaxiSchemaProvider)
//         return
//      }
//      // HttpSchemaStoreClient has a built-in retry logic, TODO - add to others.
//      logger.info("Publishing schemas")
//      val sources = localTaxiSchemaProvider.versionedSources
//      if (sources.isEmpty()) {
//         logger.error("No schemas found to publish")
//      } else {
//         logger.debug("Attempting to register schema:\n $sources")
//
//         when (val schemaValidationResult = schemaPublisher.submitSchemas(sources)) {
//            is Either.Left -> logger.error("Failed to register schema: ${schemaValidationResult.a.message}\n$sources")
//            is Either.Right -> logger.info("Schema registered successfully")
//         }
//      }
//   }
//
//   private fun publishVersionedSources(taxiProjectSourceProvider: SchemaSourceProvider) {
//      logger.info("Publishing schemas")
//      val versionedSources = taxiProjectSourceProvider.versionedSources
//      if (versionedSources.isEmpty()) {
//         logger.error("No schemas found to publish")
//      } else {
//         logger.debug("Attempting to register schema: ${versionedSources.map { it.name }}")
//         when (val schemaValidationResult = schemaPublisher.submitSchemas(versionedSources)) {
//            is Either.Left -> logger.error("Failed to register schema: ${schemaValidationResult.a.message}")
//            is Either.Right -> logger.info("Schema registered successfully")
//         }
//      }
//   }
//}
