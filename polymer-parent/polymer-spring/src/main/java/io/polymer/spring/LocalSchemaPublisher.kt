package io.polymer.spring

import io.osmosis.polymer.utils.log
import io.polymer.schemaStore.SchemaStoreClient
import javax.annotation.PostConstruct

class LocalSchemaPublisher(val localTaxiSchemaProvider: LocalTaxiSchemaProvider,
                           val schemaStoreClient: SchemaStoreClient,
                           val schemaName: String,
                           val schemaVersion: String) {

   @PostConstruct
   fun publish() {
      // TODO : Add retry logic
      log().info("Publishing schemas")
      schemaStoreClient.submitSchema(schemaName, schemaVersion, localTaxiSchemaProvider.schemaString())

   }
}
