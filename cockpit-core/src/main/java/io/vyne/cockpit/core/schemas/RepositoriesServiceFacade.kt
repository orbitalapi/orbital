package io.vyne.cockpit.core.schemas

import io.vyne.schemaServer.repositories.GitRepositoryChangeRequest
import io.vyne.schemaServer.repositories.RepositoryServiceApi
import io.vyne.spring.config.ExcludeFromOrbitalStation
import io.vyne.spring.http.handleFeignErrors
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono


// Simple pass-through to the Schema server
@RestController
@ExcludeFromOrbitalStation
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

