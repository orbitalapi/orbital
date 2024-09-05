package com.orbitalhq.schemaServer.core.repositories

import com.google.common.annotations.VisibleForTesting
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.schemaServer.core.file.FileProjectSpec
import com.orbitalhq.schemaServer.core.git.GitProjectSpec
import com.orbitalhq.schemaServer.core.git.GitUtils
import com.orbitalhq.schemaServer.packages.AvroPackageLoaderSpec
import com.orbitalhq.schemaServer.packages.OpenApiPackageLoaderSpec
import com.orbitalhq.schemaServer.packages.PackageType
import com.orbitalhq.schemaServer.packages.SoapPackageLoaderSpec
import com.orbitalhq.schemaServer.repositories.CreateFileProjectStoreRequest
import com.orbitalhq.schemaServer.repositories.FileProjectStoreTestRequest
import com.orbitalhq.schemaServer.repositories.FileProjectTestResponse
import com.orbitalhq.schemaServer.repositories.GitConnectionTestRequest
import com.orbitalhq.schemaServer.repositories.GitConnectionTestResult
import com.orbitalhq.schemaServer.repositories.git.GitProjectStoreChangeRequest
import com.orbitalhq.security.VynePrivileges
import com.orbitalhq.spring.http.BadRequestException
import mu.KotlinLogging
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.nio.file.Path
import java.nio.file.Paths

@RestController
class WorkspaceProjectsService(private val configRepo: WorkspaceConfigLoader) {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   @PreAuthorize("hasAuthority('${VynePrivileges.BrowseSchema}')")
   @GetMapping("/api/repositories")
   suspend fun listRepositoriesAsJson(): String {
      return configRepo.safeConfigJson()
   }

   // For testing, not part of the REST API
   fun listRepositories(): WorkspaceConfig {
      return configRepo.load()
   }

   @PreAuthorize("hasAuthority('${VynePrivileges.EditSchema}')")
   @PostMapping("/api/repositories/file")
   fun createFileRepository(@RequestBody request: CreateFileProjectStoreRequest): Mono<ModifyWorkspaceResponse> {
      val fileSpec = request.toRepositorySpec()
      return Mono.just(configRepo.addFileSpec(fileSpec)).map {
         if (it.status == ModifyProjectResponseStatus.Failed) {
            throw BadRequestException(it.message!!)
         } else {
            it
         }
      }
   }

   @PostMapping("/api/workspace/projects/{projectId}")
   fun createFileProject(
      @PathVariable("projectId") projectId: String,
      @RequestParam("format") packageType: PackageType,
   ) {

   }


   @VisibleForTesting
   fun removeFileRepository(repositoryPath: Path, packageIdentifier: PackageIdentifier): List<PackageIdentifier> {
      return configRepo.removeFileRepository(repositoryPath,packageIdentifier)
   }

   @PostMapping("/api/repositories/file", params = ["test"])
   @PreAuthorize("hasAuthority('${VynePrivileges.TestConnections}')")
   fun testFileProjectStore(@RequestBody request: FileProjectStoreTestRequest): Mono<FileProjectTestResponse> {
      return configRepo.validateProjectExists(request)
         .subscribeOn(Schedulers.boundedElastic())
   }


   @PostMapping("/api/repositories/git")
   @PreAuthorize("hasAuthority('${VynePrivileges.EditSchema}')")
   fun createGitProjectStore(@RequestBody request: GitProjectStoreChangeRequest): Mono<ModifyWorkspaceResponse> {
      val config = request.toRepositorySpec()
      return try {
         Mono.just(configRepo.addGitSpec(config)).map {
            if (it.status == ModifyProjectResponseStatus.Failed) {
               throw BadRequestException(it.message!!)
            } else {
               it
            }
         }
      } catch (e: Exception) {
         Mono.just(ModifyWorkspaceResponse(ModifyProjectResponseStatus.Failed, e.message))
      }
   }

   @PostMapping("/api/repositories/git", params = ["test"])
   @PreAuthorize("hasAuthority('${VynePrivileges.TestConnections}')")
   fun testGitConnection(@RequestBody request: GitConnectionTestRequest): Mono<GitConnectionTestResult> {
      return Mono.just(GitUtils.testConnection(request.uri))
         .map { testResult ->
            GitConnectionTestResult(
               successful = testResult.successful,
               errorMessage = testResult.errorMessage,
               branchNames = testResult.branchNames,
               defaultBranch = testResult.defaultBranch
            )
         }
   }
}

fun GitProjectStoreChangeRequest.toRepositorySpec(): GitProjectSpec {
   return GitProjectSpec(
      this.name,
      this.uri,
      this.branch,
      path = Paths.get(this.path),
      loader = this.loader
   )
}

fun CreateFileProjectStoreRequest.toRepositorySpec(): FileProjectSpec {
   val packageIdentifier = when (this.loader.packageType) {
      PackageType.Taxi -> this.newProjectIdentifier

      PackageType.OpenApi -> (this.loader as OpenApiPackageLoaderSpec).identifier
      PackageType.Soap -> (this.loader as SoapPackageLoaderSpec).identifier
      PackageType.Avro -> (this.loader as AvroPackageLoaderSpec).identifier
      else -> error("Package type of ${this.loader.packageType} is not yet supported")
   }


   return FileProjectSpec(
      Paths.get(path),
      isEditable = isEditable,
      packageIdentifier = packageIdentifier,
      loader = loader
   )
}
