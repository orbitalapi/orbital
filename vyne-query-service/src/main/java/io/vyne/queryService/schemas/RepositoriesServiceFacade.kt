package io.vyne.queryService.schemas

import io.vyne.queryService.utils.handleFeignErrors
import io.vyne.schemaServer.repositories.GitRepositoryChangeRequest
import io.vyne.schemaServer.repositories.RepositoryServiceApi
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono


// Simple pass-through to the Schema server
@RestController
class RepositoriesServiceFacade(private val repositoryServiceApi: RepositoryServiceApi) {

   @PostMapping("/api/repositories/git")
   fun listPackages(): Mono<Unit> = handleFeignErrors {
      repositoryServiceApi.createGitRepository(
         GitRepositoryChangeRequest(
            "taxonomy-test",
            "https://github.com/RoopeHakulinen/taxonomy-test.git",
            "main"
         )
      )
      Mono.empty() // TODO
   }
}

