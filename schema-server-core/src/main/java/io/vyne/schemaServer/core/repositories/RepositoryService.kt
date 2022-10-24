package io.vyne.schemaServer.core.repositories

import io.vyne.schemaServer.core.BadRepositorySpecException
import io.vyne.schemaServer.core.SchemaRepositoryConfig
import io.vyne.schemaServer.core.SchemaRepositoryConfigLoader
import io.vyne.schemaServer.core.file.FileSystemPackageSpec
import io.vyne.schemaServer.repositories.FileRepositoryChangeRequest
import io.vyne.schemaServer.repositories.RepositoryServiceApi
import io.vyne.spring.http.BadRequestException
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.nio.file.Paths

@RestController
class RepositoryService(private val configRepo: SchemaRepositoryConfigLoader) : RepositoryServiceApi {
   @GetMapping("/api/repositories")
   fun listRepositoriesAsJson(): String {
      return configRepo.safeConfigJson()
   }

   // For testing, not part of the REST API
   fun listRepositories(): SchemaRepositoryConfig {
      return configRepo.load()
   }


   override fun createFileRepository(request: FileRepositoryChangeRequest) {
      val fileSpec = FileSystemPackageSpec(
         Paths.get(request.path),
         editable = request.editable
      )
      try {
         configRepo.addFileSpec(fileSpec)
      } catch (e: BadRepositorySpecException) {
         throw BadRequestException(e.message!!)
      }

   }

}
