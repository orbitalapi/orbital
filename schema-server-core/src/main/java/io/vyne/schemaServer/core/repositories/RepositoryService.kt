package io.vyne.schemaServer.core.repositories

import io.vyne.schemaServer.core.file.FileSystemPackageSpec
import io.vyne.schemaServer.core.git.GitOperations
import io.vyne.schemaServer.core.git.GitRepositoryConfig
import io.vyne.schemaServer.packages.OpenApiPackageLoaderSpec
import io.vyne.schemaServer.packages.PackageType
import io.vyne.schemaServer.packages.SoapPackageLoaderSpec
import io.vyne.schemaServer.repositories.*
import io.vyne.schemaServer.repositories.git.GitRepositoryChangeRequest
import io.vyne.spring.http.BadRequestException
import io.vyne.toVynePackageIdentifier
import lang.taxi.packages.TaxiPackageLoader
import mu.KotlinLogging
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.nio.file.Paths

@RestController
class RepositoryService(private val configRepo: SchemaRepositoryConfigLoader) : RepositoryServiceApi {
   companion object {
      private val logger = KotlinLogging.logger {}
   }

   @GetMapping("/api/repositories")
   fun listRepositoriesAsJson(): String {
      return configRepo.safeConfigJson()
   }

   // For testing, not part of the REST API
   fun listRepositories(): SchemaRepositoryConfig {
      return configRepo.load()
   }

   @PostMapping("/api/repositories/file")
   override fun createFileRepository(@RequestBody request: CreateFileRepositoryRequest): Mono<Unit> {
      val packageIdentifier = when (request.loader.packageType) {
         PackageType.Taxi -> request.newProjectIdentifier

         PackageType.OpenApi -> (request.loader as OpenApiPackageLoaderSpec).identifier
         PackageType.Soap -> (request.loader as SoapPackageLoaderSpec).identifier
         else -> error("Package type of ${request.loader.packageType} is not yet supported")
      }


      val fileSpec = FileSystemPackageSpec(
         Paths.get(request.path),
         isEditable = request.isEditable,
         packageIdentifier = packageIdentifier,
         loader = request.loader
      )
      try {
         configRepo.addFileSpec(fileSpec)
      } catch (e: IllegalArgumentException) {
         throw BadRequestException(e.message!!)
      }
      return Mono.empty()
   }

   @PostMapping("/api/repositories/file/test")
   override fun testFileRepository(@RequestBody request: FileRepositoryTestRequest): Mono<FileRepositoryTestResponse> {
      return try {
         val project = TaxiPackageLoader.forDirectoryContainingTaxiFile(Paths.get(request.path)).load()
         Mono.just(FileRepositoryTestResponse(request.path, true, project.identifier.toVynePackageIdentifier()))
      } catch (e: Exception) {
         logger.info { "Could not find a package at ${request.path} - maybe it doesn't exist? Error: ${e.message}" }
         Mono.just(FileRepositoryTestResponse(request.path, false, null))
      }
   }


   @PostMapping("/api/repositories/git")
   override fun createGitRepository(request: GitRepositoryChangeRequest): Mono<Unit> {
      val config = GitRepositoryConfig(
         request.name,
         request.uri,
         request.branch,
         path = Paths.get(request.projectRootPath)
      )
      try {
         configRepo.addGitSpec(config)
      } catch (e: IllegalStateException) {
         throw BadRequestException(e.message!!)
      }
      return Mono.empty()
   }

   @PostMapping("/api/repositories/git/test")
   override fun testGitConnection(request: GitConnectionTestRequest): Mono<GitConnectionTestResult> {
      return Mono.just(GitOperations.testConnection(request.uri))
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
