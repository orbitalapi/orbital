package io.vyne.cockpit.core.schemas

import io.vyne.schemaServer.repositories.CreateFileRepositoryRequest
import io.vyne.schemaServer.repositories.FileRepositoryTestRequest
import io.vyne.schemaServer.repositories.FileRepositoryTestResponse
import io.vyne.schemaServer.repositories.GitConnectionTestRequest
import io.vyne.schemaServer.repositories.GitConnectionTestResult
import io.vyne.schemaServer.repositories.RepositoryServiceApi
import io.vyne.schemaServer.repositories.git.GitRepositoryChangeRequest
import io.vyne.spring.config.ExcludeFromOrbitalStation
import io.vyne.spring.http.handleFeignErrors
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono


// Simple pass-through to the Schema server
@RestController
@ExcludeFromOrbitalStation
class RepositoriesServiceFacade(private val repositoryServiceApi: RepositoryServiceApi) {

   @PostMapping("/api/repositories/file")
   fun createFileRepository(@RequestBody request: CreateFileRepositoryRequest): Mono<Unit> = handleFeignErrors {
      repositoryServiceApi.createFileRepository(request)
   }

   @PostMapping("/api/repositories/file", params = ["test"])
   fun testFileRepository(@RequestBody request: FileRepositoryTestRequest): Mono<FileRepositoryTestResponse> =
      handleFeignErrors {
         repositoryServiceApi.testFileRepository(request)
      }


   @PostMapping("/api/repositories/git")
   fun createGitRepository(@RequestBody request: GitRepositoryChangeRequest): Mono<Unit> = handleFeignErrors {
      repositoryServiceApi.createGitRepository(request)
   }

   @PostMapping("/api/repositories/git", params = ["test"])
   fun testGitConnection(@RequestBody request: GitConnectionTestRequest): Mono<GitConnectionTestResult> =
      handleFeignErrors {
         repositoryServiceApi.testGitConnection(request)
      }


}

