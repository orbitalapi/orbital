package com.orbitalhq.cockpit.core.schemas

import com.orbitalhq.schemaServer.repositories.CreateFileProjectStoreRequest
import com.orbitalhq.schemaServer.repositories.FileProjectStoreTestRequest
import com.orbitalhq.schemaServer.repositories.FileProjectStoreTestResponse
import com.orbitalhq.schemaServer.repositories.GitConnectionTestRequest
import com.orbitalhq.schemaServer.repositories.GitConnectionTestResult
import com.orbitalhq.schemaServer.repositories.WorkspaceServiceApi
import com.orbitalhq.schemaServer.repositories.git.GitProjectStoreChangeRequest
import com.orbitalhq.spring.config.ExcludeFromOrbitalStation
import com.orbitalhq.spring.http.handleFeignErrors
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono


// Simple pass-through to the Schema server
@RestController
@ExcludeFromOrbitalStation
class RepositoriesServiceFacade(private val workspaceServiceApi: WorkspaceServiceApi) {

   @PostMapping("/api/repositories/file")
   fun createFileRepository(@RequestBody request: CreateFileProjectStoreRequest): Mono<Unit> = handleFeignErrors {
      workspaceServiceApi.createFileRepository(request)
   }

   @PostMapping("/api/repositories/file", params = ["test"])
   fun testFileRepository(@RequestBody request: FileProjectStoreTestRequest): Mono<FileProjectStoreTestResponse> =
      handleFeignErrors {
         workspaceServiceApi.testFileProjectStore(request)
      }


   @PostMapping("/api/repositories/git")
   fun createGitRepository(@RequestBody request: GitProjectStoreChangeRequest): Mono<Unit> = handleFeignErrors {
      workspaceServiceApi.createGitProjectStore(request)
   }

   @PostMapping("/api/repositories/git", params = ["test"])
   fun testGitConnection(@RequestBody request: GitConnectionTestRequest): Mono<GitConnectionTestResult> =
      handleFeignErrors {
         workspaceServiceApi.testGitConnection(request)
      }


}

