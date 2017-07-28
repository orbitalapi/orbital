package io.polymer.schemaStore

import io.osmosis.polymer.utils.log
import reactor.core.Disposable
import reactor.core.publisher.Flux
import java.time.Duration


class SchemaStoreClient(val schemaService: SchemaService, val pollFrequency: Duration = Duration.ofSeconds(1L)) {

   var poller: Disposable? = null
   var schemaSet: SchemaSet = SchemaSet.EMPTY
      private set

   fun submitSchema(schemaName: String,
                    schemaVersion: String,
                    schema: String) {
      log().debug("Submitting schema $schemaName v$schemaVersion")
      schemaService.submitSchema(schema, schemaName, schemaVersion)
      log().debug("Schema $schemaName v$schemaVersion submitted successfully")
   }


   fun startPolling() {
      poller = Flux.interval(pollFrequency)
         .doOnNext { pollForSchemaUpdates() }
         .subscribe()
   }

   fun stopPolling() {
      poller?.dispose()
   }

   fun pollForSchemaUpdates() {
      try {
         val schemaSet = schemaService.listSchemas()
         if (this.schemaSet.id != schemaSet.id) {
            log().info("Updated to schema set ${schemaSet.id} (contains ${schemaSet.size()} schemas)")
         }
      } catch (e: Exception) {
         log().warn("Failed to fetch schemas: $e")
      }
   }
}
