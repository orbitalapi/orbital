package io.vyne.schemaServer.core.repositories

import io.vyne.schemaServer.core.SchemaRepositoryConfigLoader
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class RepositoryService(private val configRepo: io.vyne.schemaServer.core.SchemaRepositoryConfigLoader) {
   @GetMapping("/api/repositories")
   fun listRepositories(): String {
      return configRepo.safeConfigJson()
   }
}
