package io.vyne.schemaServer.repositories

import io.vyne.PackageIdentifier
import io.vyne.PackageMetadata
import org.springframework.web.bind.annotation.PostMapping
import reactivefeign.spring.config.ReactiveFeignClient
import reactor.core.publisher.Mono

@ReactiveFeignClient("\${vyne.schema-server.name:schema-server-repositories}", qualifier = "repositoryFeignClient")
interface RepositoryServiceApi {

   @PostMapping("/api/repositories/file")
   fun createFileRepository(request: CreateFileRepositoryRequest): Mono<Unit> // TODO Fix typings

   @PostMapping("/api/repositories/git")
   fun createGitRepository(request: GitRepositoryChangeRequest): Mono<Unit>
}


data class CreateFileRepositoryRequest(
   val path: String,
   val editable: Boolean,
   val packageIdentifier: PackageIdentifier
)

data class GitRepositoryChangeRequest(
   val name: String,
   val uri: String,
   val branch: String,

   val projectRootPath: String = "/"
)

