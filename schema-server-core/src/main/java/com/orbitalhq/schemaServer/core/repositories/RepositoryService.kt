package com.orbitalhq.schemaServer.core.repositories

import com.orbitalhq.schemaServer.core.file.FileSystemPackageSpec
import com.orbitalhq.schemaServer.core.git.GitOperations
import com.orbitalhq.schemaServer.core.git.GitRepositorySpec
import com.orbitalhq.schemaServer.packages.OpenApiPackageLoaderSpec
import com.orbitalhq.schemaServer.packages.PackageType
import com.orbitalhq.schemaServer.packages.SoapPackageLoaderSpec
import com.orbitalhq.schemaServer.repositories.*
import com.orbitalhq.schemaServer.repositories.git.GitRepositoryChangeRequest
import com.orbitalhq.spring.http.BadRequestException
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
        val fileSpec = request.toRepositorySpec()
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
        val config = request.toRepositorySpec()
        try {
            configRepo.addGitSpec(config)
        } catch (e: IllegalStateException) {
            throw BadRequestException(e.message!!)
        }
        return Mono.empty()
    }

    @PostMapping("/api/repositories/git", params = ["test"])
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

fun GitRepositoryChangeRequest.toRepositorySpec(): GitRepositorySpec {
    return GitRepositorySpec(
        this.name,
        this.uri,
        this.branch,
        path = Paths.get(this.projectRootPath)
    )
}

fun CreateFileRepositoryRequest.toRepositorySpec(): FileSystemPackageSpec {
    val packageIdentifier = when (this.loader.packageType) {
        PackageType.Taxi -> this.newProjectIdentifier

        PackageType.OpenApi -> (this.loader as OpenApiPackageLoaderSpec).identifier
        PackageType.Soap -> (this.loader as SoapPackageLoaderSpec).identifier
        else -> error("Package type of ${this.loader.packageType} is not yet supported")
    }


    return FileSystemPackageSpec(
        Paths.get(path),
        isEditable = isEditable,
        packageIdentifier = packageIdentifier,
        loader = loader
    )
}
