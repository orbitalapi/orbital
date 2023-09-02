package com.orbitalhq.cockpit.core.schemas

import com.orbitalhq.schemaServer.repositories.CreateFileRepositoryRequest
import com.orbitalhq.schemaServer.repositories.FileRepositoryTestRequest
import com.orbitalhq.schemaServer.repositories.FileRepositoryTestResponse
import com.orbitalhq.schemaServer.repositories.GitConnectionTestRequest
import com.orbitalhq.schemaServer.repositories.GitConnectionTestResult
import com.orbitalhq.schemaServer.repositories.RepositoryServiceApi
import com.orbitalhq.schemaServer.repositories.git.GitRepositoryChangeRequest
import com.orbitalhq.spring.config.ExcludeFromOrbitalStation
import com.orbitalhq.spring.http.handleFeignErrors
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

