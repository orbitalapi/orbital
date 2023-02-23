package io.vyne.schemaServer.repositories

import io.vyne.PackageIdentifier
import io.vyne.schemaServer.packages.PackageLoaderSpec
import io.vyne.schemaServer.repositories.git.GitRepositoryChangeRequest
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import reactivefeign.spring.config.ReactiveFeignClient
import reactor.core.publisher.Mono

@ReactiveFeignClient("\${vyne.schema-server-repositories.name:schema-server}", qualifier = "repositoryFeignClient")
interface RepositoryServiceApi {

   @PostMapping("/api/repositories/file")
   fun createFileRepository(@RequestBody request: CreateFileRepositoryRequest): Mono<Unit>

   @PostMapping("/api/repositories/file", params = ["test"])
   fun testFileRepository(@RequestBody request: FileRepositoryTestRequest): Mono<FileRepositoryTestResponse>


   @PostMapping("/api/repositories/git")
   fun createGitRepository(@RequestBody request: GitRepositoryChangeRequest): Mono<Unit>

   @PostMapping("/api/repositories/git", params = ["test"])
   fun testGitConnection(@RequestBody request: GitConnectionTestRequest): Mono<GitConnectionTestResult>

}

data class FileRepositoryTestRequest(
   val path: String
)

data class FileRepositoryTestResponse(
   val path: String,
   val exists: Boolean,
   val identifier: PackageIdentifier?
)

data class GitConnectionTestRequest(
   val uri: String,
)

data class GitConnectionTestResult(
   val successful: Boolean,
   val errorMessage: String?,
   val branchNames: List<String>?,
   val defaultBranch: String?

)

data class CreateFileRepositoryRequest(
   val path: String,
   val isEditable: Boolean,
   val loader: PackageLoaderSpec,

   /**
    * If populated, indicates that the path is not
    * expected to contain a project, and a new project will be created
    * using the provided package identifier.
    *
    * If empty, it is expected that a project already
    * exists at the provided path, and the packageIdentifier should
    * be read from the provided location.
    *
    * Only relevant for Taxi projects
    */
   val newProjectIdentifier: PackageIdentifier? = null
)

