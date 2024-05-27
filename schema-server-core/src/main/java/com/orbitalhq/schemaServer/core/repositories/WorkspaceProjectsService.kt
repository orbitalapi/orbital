package com.orbitalhq.schemaServer.core.repositories

import com.orbitalhq.schemaServer.core.file.FileSystemPackageSpec
import com.orbitalhq.schemaServer.core.git.GitProjectStoreSpec
import com.orbitalhq.schemaServer.core.git.GitUtils
import com.orbitalhq.schemaServer.packages.AvroPackageLoaderSpec
import com.orbitalhq.schemaServer.packages.OpenApiPackageLoaderSpec
import com.orbitalhq.schemaServer.packages.PackageType
import com.orbitalhq.schemaServer.packages.SoapPackageLoaderSpec
import com.orbitalhq.schemaServer.repositories.*
import com.orbitalhq.schemaServer.repositories.git.GitProjectStoreChangeRequest
import com.orbitalhq.toVynePackageIdentifier
import lang.taxi.packages.TaxiPackageLoader
import mu.KotlinLogging
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.nio.file.Paths

@RestController
class WorkspaceProjectsService(private val configRepo: WorkspaceConfigLoader) {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   @GetMapping("/api/repositories")
   fun listRepositoriesAsJson(): String {
      return configRepo.safeConfigJson()
   }

   // For testing, not part of the REST API
   fun listRepositories(): WorkspaceConfig {
      return configRepo.load()
   }

   @PostMapping("/api/repositories/file")
   fun createFileRepository(@RequestBody request: CreateFileProjectStoreRequest): Mono<ModifyWorkspaceResponse> {
      val fileSpec = request.toRepositorySpec()
      return Mono.just(configRepo.addFileSpec(fileSpec))
   }

   @PostMapping("/api/repositories/file", params = ["test"])
   fun testFileProjectStore(@RequestBody request: FileProjectStoreTestRequest): Mono<FileProjectStoreTestResponse> {
      return try {
         val project = TaxiPackageLoader.forDirectoryContainingTaxiFile(Paths.get(request.path)).load()
         Mono.just(FileProjectStoreTestResponse(request.path, true, project.identifier.toVynePackageIdentifier()))
      } catch (e: Exception) {
         logger.info { "Could not find a package at ${request.path} - maybe it doesn't exist? Error: ${e.message}" }
         Mono.just(FileProjectStoreTestResponse(request.path, false, null))
      }
   }


   @PostMapping("/api/repositories/git")
   fun createGitProjectStore(@RequestBody request: GitProjectStoreChangeRequest): Mono<ModifyWorkspaceResponse> {
      val config = request.toRepositorySpec()
      return try {
         Mono.just(configRepo.addGitSpec(config))
      } catch (e: Exception) {
         Mono.just(ModifyWorkspaceResponse(ModifyProjectResponseStatus.Failed, e.message))
      }
   }

   @PostMapping("/api/repositories/git", params = ["test"])
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

fun GitProjectStoreChangeRequest.toRepositorySpec(): GitProjectStoreSpec {
   return GitProjectStoreSpec(
      this.name,
      this.uri,
      this.branch,
      path = Paths.get(this.path),
      loader = this.loader
   )
}

fun CreateFileProjectStoreRequest.toRepositorySpec(): FileSystemPackageSpec {
   val packageIdentifier = when (this.loader.packageType) {
      PackageType.Taxi -> this.newProjectIdentifier

      PackageType.OpenApi -> (this.loader as OpenApiPackageLoaderSpec).identifier
      PackageType.Soap -> (this.loader as SoapPackageLoaderSpec).identifier
      PackageType.Avro -> (this.loader as AvroPackageLoaderSpec).identifier
      else -> error("Package type of ${this.loader.packageType} is not yet supported")
   }


   return FileSystemPackageSpec(
      Paths.get(path),
      isEditable = isEditable,
      packageIdentifier = packageIdentifier,
      loader = loader
   )
}
