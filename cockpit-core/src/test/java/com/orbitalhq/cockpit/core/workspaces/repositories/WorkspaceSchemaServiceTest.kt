package com.orbitalhq.cockpit.core.workspaces.repositories

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.cockpit.core.DatabaseTest
import com.orbitalhq.cockpit.core.auth.WorkspaceServiceTest
import com.orbitalhq.cockpit.core.auth.authForUserId
import com.orbitalhq.schemaServer.core.repositories.SchemaRepositoryConfig
import com.orbitalhq.schemaServer.core.repositories.SchemaRepositoryConfigLoader
import com.orbitalhq.schemaServer.core.repositories.lifecycle.RepositorySpecLifecycleEventDispatcher
import com.orbitalhq.schemaServer.packages.TaxiPackageLoaderSpec
import com.orbitalhq.schemaServer.repositories.CreateFileRepositoryRequest
import com.orbitalhq.schemaServer.repositories.git.GitRepositoryChangeRequest
import io.kotest.matchers.nulls.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [WorkspaceSchemaServiceTest.Companion.Config::class])
class WorkspaceSchemaServiceTest : DatabaseTest() {

    companion object {
        @SpringBootConfiguration
        @EntityScan(basePackageClasses = [WorkspaceSchemaSpec::class])
        @EnableJpaRepositories(basePackageClasses = [WorkspaceSchemaSpecRepository::class])
        class Config
    }

    @Autowired
    lateinit var workspaceSchemaSpecRepository: WorkspaceSchemaSpecRepository

    lateinit var service: WorkspaceSchemaService
    lateinit var eventDispatcher: RepositorySpecLifecycleEventDispatcher

    @BeforeEach
    fun setup() {
        val schemaConfigLoader = mock<SchemaRepositoryConfigLoader>() {
            on { load() } doReturn SchemaRepositoryConfig()
        }
        eventDispatcher = mock<RepositorySpecLifecycleEventDispatcher>()
        service = WorkspaceSchemaService(
            workspaceSchemaSpecRepository,
            eventDispatcher,
            schemaConfigLoader
        )
    }

    @Test
    fun `adding a new file repo gets persisted to disk`() {
        val added = service.doAddFileRepoToWorkspace(
            1,2, authForUserId("marty"), CreateFileRepositoryRequest(
                "path/to/repo",
                true,
                TaxiPackageLoaderSpec,
                PackageIdentifier.fromId("com.test/foo/0.1.0")
            )
        )
        workspaceSchemaSpecRepository.findByIdOrNull(added.id).shouldNotBeNull()
        verify(eventDispatcher).fileRepositorySpecAdded(any())
    }

    @Test
    fun `adding a new git repo gets persisted to disk`() {
        val added = service.doAddGitRepoToWorkspace(
            1,2, authForUserId("marty"), GitRepositoryChangeRequest(
                "test",
                "https://git.com/test",
                "main"
            )
        )
        workspaceSchemaSpecRepository.findByIdOrNull(added.id).shouldNotBeNull()
        verify(eventDispatcher).gitRepositorySpecAdded(any())
    }
}
