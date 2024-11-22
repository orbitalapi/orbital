package com.orbitalhq.schemaServer.core.repositories

import com.google.common.annotations.VisibleForTesting
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.schemaServer.core.file.FileProjectSpec
import com.orbitalhq.schemaServer.core.git.GitProjectSpec
import com.orbitalhq.schemaServer.core.git.GitUtils
import com.orbitalhq.schemaServer.core.repositories.upload.ProjectUploadHandler
import com.orbitalhq.schemaServer.packages.AvroPackageLoaderSpec
import com.orbitalhq.schemaServer.packages.OpenApiPackageLoaderSpec
import com.orbitalhq.schemaServer.packages.PackageType
import com.orbitalhq.schemaServer.packages.SoapPackageLoaderSpec
import com.orbitalhq.schemaServer.repositories.AddFileProjectRequest
import com.orbitalhq.schemaServer.repositories.CreateEmptyProjectRequest
import com.orbitalhq.schemaServer.repositories.FileProjectStoreTestRequest
import com.orbitalhq.schemaServer.repositories.FileProjectTestResponse
import com.orbitalhq.schemaServer.repositories.GitConnectionTestRequest
import com.orbitalhq.schemaServer.repositories.GitConnectionTestResult
import com.orbitalhq.schemaServer.repositories.git.GitProjectStoreChangeRequest
import com.orbitalhq.security.VynePrivileges
import com.orbitalhq.spring.http.BadRequestException
import mu.KotlinLogging
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.util.MultiValueMap
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
class WorkspaceProjectsService(
   private val configRepo: WorkspaceConfigLoader,
   private val projectUploadHandlers: List<ProjectUploadHandler> = ProjectUploadHandler.DEFAULT
) {
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

   @Deprecated("Deprecated as a REST API - internal calls to the method are fine - This approach is awkward for users around file handling - use uploadProject instead")
   @PreAuthorize("hasAuthority('${VynePrivileges.EditSchema}')")
   @PostMapping("/api/repositories/file")
   fun createFileRepository(@RequestBody request: AddFileProjectRequest): Mono<ModifyWorkspaceResponse> {
      val fileSpec = request.toRepositorySpec()
      return Mono.just(configRepo.addFileSpec(fileSpec)).map {
         if (it.status == ModifyProjectResponseStatus.Failed) {
            throw BadRequestException(it.message!!)
         } else {
            it
         }
      }
   }

   @PreAuthorize("hasAuthority('${VynePrivileges.EditSchema}')")
   @PostMapping("/api/repositories/new")
   fun createNewEmptyRepository(@RequestBody request: CreateEmptyProjectRequest): Mono<ModifyWorkspaceResponse> {
      val fileSpec = request.toRepositorySpec()
      return Mono.just(configRepo.addFileSpec(fileSpec)).map {
         if (it.status == ModifyProjectResponseStatus.Failed) {
            throw BadRequestException(it.message!!)
         } else {
            it
         }
      }
   }


   /**
    * Allows users to upload a project or spec which will get created as a
    * project within the workspace.
    *
    * A new project directory is created on the server, and the contents are saved there
    */
   @PreAuthorize("hasAuthority('${VynePrivileges.EditSchema}')")
   @PostMapping("/api/workspace/projects/{projectId}")
   fun uploadProject(
      @PathVariable("projectId") uriSafeProjectId: String,
      @RequestParam("format") packageType: PackageType,
      @RequestParam parameters: MultiValueMap<String, String>,
      @RequestBody payload: ByteArray
   ): Mono<ModifyWorkspaceResponse> {
      logger.info { "Attempting to import $packageType project $uriSafeProjectId" }
      val packageIdentifier = PackageIdentifier.fromUriSafeId(uriSafeProjectId)
      val workspaceProjectsRoot = configRepo.load()
         .fileConfigOrDefault
         .newProjectsPath

      val projectRoot = workspaceProjectsRoot.resolve(packageIdentifier.unversionedId.replace(".", "/"))
      logger.info { "Project $uriSafeProjectId will be saved to $projectRoot" }
      projectRoot.toFile().mkdirs()

      val uploadHandler = projectUploadHandlers.firstOrNull { it.packageType == packageType }
         ?: throw BadRequestException("Upload of projects with format of $packageType is not supported")

      val createProjectRequest = uploadHandler.processUpload(packageIdentifier, parameters.toMap(), payload, projectRoot)
      return createFileRepository(createProjectRequest)

   }


   @VisibleForTesting
   fun removeFileRepository(repositoryPath: Path, packageIdentifier: PackageIdentifier): List<PackageIdentifier> {
      return configRepo.removeFileRepository(repositoryPath, packageIdentifier)
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
      return try {
         Mono.just(GitUtils.testConnection(request.uri))
            .map { testResult ->
               GitConnectionTestResult(
                  successful = testResult.successful,
                  errorMessage = testResult.errorMessage,
                  branchNames = testResult.branchNames,
                  defaultBranch = testResult.defaultBranch
               )
            }
      } catch (e: Exception) {
         Mono.just(
            GitConnectionTestResult(
               successful = false,
               errorMessage = e.cause.toString(),
               branchNames = null,
               defaultBranch = null
            )
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

fun CreateEmptyProjectRequest.toRepositorySpec(): FileProjectSpec {

   return FileProjectSpec(
      Paths.get("workspace/projects/${this.newProjectIdentifier.id}"),
      isEditable = true,
      packageIdentifier = this.newProjectIdentifier,
   )
}

fun AddFileProjectRequest.toRepositorySpec(): FileProjectSpec {
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
