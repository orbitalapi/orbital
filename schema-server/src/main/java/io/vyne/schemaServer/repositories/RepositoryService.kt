package io.vyne.schemaServer.repositories

import io.vyne.schemaServer.FileSchemaRepositoryConfigLoader
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class RepositoryService(private val configRepo: FileSchemaRepositoryConfigLoader) {
   @GetMapping("/api/repositories")
   fun listRepositories(): String {
      return configRepo.safeConfigJson()
   }
}
