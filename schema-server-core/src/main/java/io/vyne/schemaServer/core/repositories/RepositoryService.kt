package io.vyne.schemaServer.core.repositories

import io.vyne.schemaServer.core.file.FileSystemPackageSpec
import io.vyne.schemaServer.core.git.GitRepositoryConfig
import io.vyne.schemaServer.repositories.CreateFileRepositoryRequest
import io.vyne.schemaServer.repositories.GitRepositoryChangeRequest
import io.vyne.schemaServer.repositories.RepositoryServiceApi
import io.vyne.spring.http.BadRequestException
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
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


   override fun createFileRepository(request: CreateFileRepositoryRequest): Mono<Unit> {
      val fileSpec = FileSystemPackageSpec(
         Paths.get(request.path),
         isEditable = request.editable,
         packageIdentifier = request.packageIdentifier
      )
      try {
         configRepo.addFileSpec(fileSpec)
      } catch (e: IllegalArgumentException) {
         throw BadRequestException(e.message!!)
      }
      return Mono.empty()
   }

   override fun createGitRepository(request: GitRepositoryChangeRequest): Mono<Unit> {
      val config = GitRepositoryConfig(
         request.name,
         request.uri,
         request.branch,
         path = Paths.get(request.projectRootPath)
      )
      try {
         configRepo.addGitSpec(config)
      } catch (e: IllegalStateException) {
         throw BadRequestException(e.message!!)
      }
      return Mono.empty()
   }

}
