package io.vyne.schemaServer.repositories

import io.vyne.PackageIdentifier
import io.vyne.PackageMetadata
import org.springframework.web.bind.annotation.PostMapping
import reactivefeign.spring.config.ReactiveFeignClient

@ReactiveFeignClient("\${vyne.schema-server.name:schema-server}", qualifier = "schemaEditorFeignClient")
interface RepositoryServiceApi {

   @PostMapping("/api/repositories/file")
   fun createFileRepository(request: CreateFileRepositoryRequest)

   @PostMapping("/api/repositories/git")
   fun createGitRepository(request: GitRepositoryChangeRequest)
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

