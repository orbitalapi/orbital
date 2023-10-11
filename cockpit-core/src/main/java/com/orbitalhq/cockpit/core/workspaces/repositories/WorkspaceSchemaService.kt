package com.orbitalhq.cockpit.core.workspaces.repositories

import com.orbitalhq.cockpit.core.auth.requireIsAuthenticated
import com.orbitalhq.config.getSafeConfigString
import com.orbitalhq.config.toHocon
import com.orbitalhq.schemaServer.core.repositories.SchemaRepositoryConfig
import com.orbitalhq.schemaServer.core.repositories.SchemaRepositoryConfigLoader
import com.orbitalhq.schemaServer.core.repositories.lifecycle.FileSpecAddedEvent
import com.orbitalhq.schemaServer.core.repositories.lifecycle.GitSpecAddedEvent
import com.orbitalhq.schemaServer.core.repositories.lifecycle.RepositorySpecLifecycleEventDispatcher
import com.orbitalhq.schemaServer.core.repositories.toRepositorySpec
import com.orbitalhq.schemaServer.repositories.CreateFileRepositoryRequest
import com.orbitalhq.schemaServer.repositories.git.GitRepositoryChangeRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.springframework.security.core.Authentication
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

/**
 * Service focusses on adding / removing schema specs to/from a workspace.
 * Unlike the approach in schema-server core, this service is intended
 * to be backed by a database, and consider org/workspace hierarchy
 */
@RestController
class WorkspaceSchemaService(
    private val workspaceSchemaSpecRepository: WorkspaceSchemaSpecRepository,
    private val eventDispatcher: RepositorySpecLifecycleEventDispatcher,

    /**
     * This is the thing that loads the HOCON from disk.
     * Even though things like actual project/repository config is
     * being persisted in the db, settings like where to check out git repos, and
     * sync frequency still come from this file.
     *
     * In time, this could probably do with some seperation.
     */
    private val schemaConfigLoader: SchemaRepositoryConfigLoader
) {

    companion object {
        private val logger = KotlinLogging.logger {}
    }


    @PostMapping("/api/workspaces/{orgId}/{workspaceId}/repos/file")
    suspend fun addFileRepositoryToWorkspace(
        @PathVariable("orgId") organisationId: Long,
        @PathVariable("workspaceId") workspaceId: Long,
        @AuthenticationPrincipal auth: Mono<Authentication>,
        @RequestBody request: CreateFileRepositoryRequest
    ): WorkspaceSchemaSpec = withContext(Dispatchers.IO) {
        val authentication = auth.requireIsAuthenticated()

        val spec = doAddFileRepoToWorkspace(organisationId, workspaceId, authentication, request)
        logger.info { "WorkspaceSchemaSpec ${spec.id} created by user ${authentication.name}" }
        spec
    }

    internal fun doAddFileRepoToWorkspace(
        organisationId: Long,
        workspaceId: Long,
        auth: Authentication,
        request: CreateFileRepositoryRequest
    ): WorkspaceSchemaSpec {
        val filePackageSpec = request.toRepositorySpec()
        val description = filePackageSpec.packageIdentifier!!.id
        logger.info { "User ${auth.name} is adding a new file repository $description to org/workspace ${organisationId}/${workspaceId} " }
        val configAsHoconString = filePackageSpec.toHocon().getSafeConfigString()
        val (schemaConfig, saved) = saveRepositorySpec(
            configAsHoconString, organisationId, workspaceId, WorkspaceSchemaSpec.SchemaSpecKind.File,
            description
        )

        logger.info { "Sending File repo added event for repository at $description" }
        eventDispatcher.fileRepositorySpecAdded(
            FileSpecAddedEvent(
                filePackageSpec, schemaConfig.fileConfigOrDefault
            )
        )

        return saved
    }

    private fun saveRepositorySpec(
        configAsHoconString: String,
        organisationId: Long,
        workspaceId: Long,
        specKind: WorkspaceSchemaSpec.SchemaSpecKind,
        description: String
    ): Pair<SchemaRepositoryConfig, WorkspaceSchemaSpec> {
        val schemaConfig = schemaConfigLoader.load()

        val saved = workspaceSchemaSpecRepository.save(
            WorkspaceSchemaSpec(
                id = 0,
                WorkspaceSchemaSpec.SchemaSpecKind.File,
                configAsHoconString
            )
        )
        logger.info { "Workspace schema spec ${saved.id} created for $specKind repository $description in org/workspace ${organisationId}/${workspaceId} " }
        return Pair(schemaConfig, saved)
    }

    @PostMapping("/api/workspaces/{orgId}/{workspaceId}/repos/git")
    suspend fun addGitRepositoryToWorkspace(
        @PathVariable("orgId") organisationId: Long,
        @PathVariable("workspaceId") workspaceId: Long,
        @AuthenticationPrincipal auth: Mono<Authentication>,
        @RequestBody request: GitRepositoryChangeRequest
    ): WorkspaceSchemaSpec = withContext(Dispatchers.IO) {
        val authentication = auth.requireIsAuthenticated()

        val spec = doAddGitRepoToWorkspace(organisationId, workspaceId, authentication, request)
        logger.info { "WorkspaceSchemaSpec ${spec.id} created by user ${authentication.name}" }
        spec
    }

    internal fun doAddGitRepoToWorkspace(
        organisationId: Long,
        workspaceId: Long,
        auth: Authentication,
        request: GitRepositoryChangeRequest
    ): WorkspaceSchemaSpec {
        val gitRepoConfig = request.toRepositorySpec()
        logger.info { "User ${auth.name} is adding a new git repository ${gitRepoConfig.redactedUri} to org/workspace ${organisationId}/${workspaceId} " }

        val (schemaConfig, saved) = saveRepositorySpec(
            gitRepoConfig.toHocon().getSafeConfigString(),
            organisationId,
            workspaceId,
            WorkspaceSchemaSpec.SchemaSpecKind.Git,
            gitRepoConfig.redactedUri
        )

        logger.info { "Sending Git repo added event for repository at ${gitRepoConfig.redactedUri}" }
        eventDispatcher.gitRepositorySpecAdded(
            GitSpecAddedEvent(
                gitRepoConfig, schemaConfig.gitConfigOrDefault
            )
        )

        return saved
    }
}
