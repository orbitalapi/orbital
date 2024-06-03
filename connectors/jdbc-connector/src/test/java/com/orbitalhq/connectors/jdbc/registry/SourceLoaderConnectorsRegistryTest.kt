package com.orbitalhq.connectors.jdbc.registry

import com.jayway.awaitility.Awaitility
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.connectors.config.SourceLoaderConnectorsRegistry
import com.orbitalhq.connectors.config.jdbc.DefaultJdbcConnectionConfiguration
import com.orbitalhq.connectors.config.jdbc.JdbcDriver
import com.orbitalhq.schema.consumer.ProjectManagerConfigSourceLoader
import com.orbitalhq.schema.consumer.SimpleSchemaStore
import com.orbitalhq.schemaServer.core.file.FileChangeDetectionMethod
import com.orbitalhq.schemaServer.core.file.packages.FileSystemPackageLoaderFactory
import com.orbitalhq.schemaServer.core.git.GitProjectStoreSpec
import com.orbitalhq.schemaServer.core.git.GitSchemaPackageLoaderFactory
import com.orbitalhq.schemaServer.core.git.GitSchemaRepositoryConfig
import com.orbitalhq.schemaServer.core.git.GitWriterDecorator
import com.orbitalhq.schemaServer.core.git.packages.BaseGitTest
import com.orbitalhq.schemaServer.core.repositories.InMemoryWorkspaceConfigLoader
import com.orbitalhq.schemaServer.core.repositories.WorkspaceConfig
import com.orbitalhq.schemaServer.core.repositories.WorkspaceProjectsService
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ProjectStoreLifecycleManager
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ReactiveProjectStoreManager
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.file.shouldBeAFile
import io.kotest.matchers.file.shouldExist
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import lang.taxi.packages.TaxiProjectLoader
import org.junit.Test
import java.time.Duration

class SourceLoaderConnectorsRegistryTest : BaseGitTest() {


   @Test
   fun `when adding a connection to a taxi project without additional sources declared then the config block is added`() {
      // Setup...

      // Create a git project locally
      deployTestProjectToRemoteGitPath(projectName = "sample-project-no-additional-sources")

      // Configure the stack to watch the git repo, and use it as a source
      // for config
      val projectStoreManager = buildProjectStoreManager()
      val gitLoader = projectStoreManager.gitLoaders.single()
      waitForSuccessfulGitClone(projectStoreManager, gitLoader)

      val configSource = ProjectManagerConfigSourceLoader(
         SimpleSchemaStore(),
         projectStoreManager,
         filePattern = "connections.conf"
      )
      val connectorRegistry = SourceLoaderJdbcConnectionRegistry(
         SourceLoaderConnectorsRegistry(listOf(configSource), listOf(configSource))
      )

      // test: add a JDBC connection
      val result = connectorRegistry.register(
         PackageIdentifier.fromId("taxi/sample/0.3.0"),
         DefaultJdbcConnectionConfiguration(
            "testConfig",
            JdbcDriver.POSTGRES,
            emptyMap()
         )
      )
      // Ensure we send the warning to the UI
      result.messages.shouldContain(GitWriterDecorator.GIT_COMMIT_NEEDED)

      // Test that the connections file has been created
      val writtenConfigFile = localRepoDir.root.resolve("test-git-repo/orbital/config/connections.conf")
      writtenConfigFile.shouldBeAFile()
      writtenConfigFile.shouldExist()


      val taxiConfFile = localRepoDir.root.resolve("test-git-repo/taxi.conf")
      val loadedTaxiConf = TaxiProjectLoader(taxiConfFile.toPath())
         .load()
      loadedTaxiConf.additionalSources.shouldContainKey("@orbital/config")
      loadedTaxiConf.additionalSources["@orbital/config"].shouldBe("orbital/config/*.conf")
   }

   @Test
   fun `can add a jdbc connection to a project loaded from a git repository`() {
      // Setup...

      // Create a git project locally
      deployTestProjectToRemoteGitPath()

      // Configure the stack to watch the git repo, and use it as a source
      // for config
      val projectStoreManager = buildProjectStoreManager()
      val gitLoader = projectStoreManager.gitLoaders.single()
      waitForSuccessfulGitClone(projectStoreManager, gitLoader)

      if (projectStoreManager.unhealthyLoaders.isNotEmpty()) {
         Thread.sleep(1000)
      }
      projectStoreManager.unhealthyLoaders.shouldBeEmpty()

      val configSource = ProjectManagerConfigSourceLoader(
         SimpleSchemaStore(),
         projectStoreManager,
         filePattern = "connections.conf"
      )
      val connectorRegistry = SourceLoaderJdbcConnectionRegistry(
         SourceLoaderConnectorsRegistry(listOf(configSource), listOf(configSource))
      )

      // test: add a JDBC connection
      val result = connectorRegistry.register(
         PackageIdentifier.fromId("taxi/sample/0.3.0"),
         DefaultJdbcConnectionConfiguration(
            "testConfig",
            JdbcDriver.POSTGRES,
            emptyMap()
         )
      )

      // Ensure we send the warning to the UI
      result.messages.shouldContain(GitWriterDecorator.GIT_COMMIT_NEEDED)

      val writtenConfigFile = localRepoDir.root.resolve("test-git-repo/orbital/config/connections.conf")
      writtenConfigFile.shouldBeAFile()
      writtenConfigFile.shouldExist()
      writtenConfigFile.readText()
         .shouldBe(
            """jdbc {
    testConfig {
        connectionName=testConfig
        connectionParameters {}
        jdbcDriver=POSTGRES
    }
}
"""
         )
   }


   private fun buildProjectStoreManager(): ReactiveProjectStoreManager {
      // Setup: Loading the config from disk
      val eventDispatcher = ProjectStoreLifecycleManager()
//      val loader = FileSchemaRepositoryConfigLoader(configFile.toPath(), eventDispatcher = eventDispatcher)
      val loader = InMemoryWorkspaceConfigLoader(
         WorkspaceConfig(
            git = GitSchemaRepositoryConfig(
               checkoutRoot = localRepoDir.root.toPath(),
               repositories = listOf(
                  GitProjectStoreSpec(
                     "test-git-repo",
                     uri = remoteRepoDir.root.toURI().toASCIIString(),
                     branch = "master",
                     isEditable = true
                  )
               )
            )
         ),
         eventDispatcher
      )
      val workspaceProjectsService = WorkspaceProjectsService(loader)

      // Setup: Building the repository manager, which should
      // create new repositories as config is added
      val repositoryManager = ReactiveProjectStoreManager(
         FileSystemPackageLoaderFactory(),
         GitSchemaPackageLoaderFactory(
            changeDetectionMethod = FileChangeDetectionMethod.POLL,
            pollFrequency = Duration.ofDays(1)
         ),
         eventDispatcher, eventDispatcher, eventDispatcher
      )

      return repositoryManager
   }
}
