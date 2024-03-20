package com.orbitalhq.cockpit.core.connectors.nosql

import com.orbitalhq.connectors.nosql.mongodb.MongoConnectionFactory
import com.orbitalhq.connectors.nosql.mongodb.registry.MongoConnectionRegistry
import com.orbitalhq.security.VynePrivileges
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

@RestController
class MongoController(
   private val mongoConnectionRegistry: MongoConnectionRegistry,
   private val mongoConnectionFactory: MongoConnectionFactory
) {
   @PreAuthorize("hasAuthority('${VynePrivileges.ViewConnections}')")
   @GetMapping("/api/connections/mongo/{connectionName}/collections")
   fun listConnectionCollections(@PathVariable("connectionName") connectionName: String): Flux<String> {
      val connectionConfiguration = mongoConnectionRegistry.getConnection(connectionName)
      return mongoConnectionFactory
         .reactiveMongoTemplate(connectionConfiguration)
         .collectionNames
   }
}
