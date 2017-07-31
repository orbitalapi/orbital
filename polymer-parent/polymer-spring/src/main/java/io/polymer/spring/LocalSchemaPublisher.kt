package io.polymer.spring

import io.osmosis.polymer.utils.log
import io.polymer.schemaStore.SchemaStoreClient
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import javax.annotation.PostConstruct

class LocalSchemaPublisher(val schemaName: String,
                           val schemaVersion: String,
                           val localTaxiSchemaProvider: LocalTaxiSchemaProvider,
                           val schemaStoreClient: SchemaStoreClient) {
   private var startupPublishTriggered:Boolean = false
   @PostConstruct
   fun onPC() {
      log().info("PostConstruct")
   }

   @EventListener
   fun handleEvent(event: ContextRefreshedEvent) {
      if (!startupPublishTriggered) {
         startupPublishTriggered = true
         publish()
      }

   }
   fun publish() {
      // TODO : Add retry logic
      log().info("Publishing schemas")
      schemaStoreClient.submitSchema(schemaName, schemaVersion, localTaxiSchemaProvider.schemaString())
      log().info("Schemas published")
   }
}
