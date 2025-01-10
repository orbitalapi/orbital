package com.orbitalhq.connectors.jdbc.registry

import com.google.common.io.Resources
import com.jayway.awaitility.Awaitility
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.config.ConfigFileLocationConventions
import com.orbitalhq.config.FileConfigSourceLoader
import com.orbitalhq.connectors.config.SourceLoaderConnectorsRegistry
import com.orbitalhq.connectors.config.jdbc.DefaultJdbcConnectionConfiguration
import com.orbitalhq.schema.consumer.ProjectManagerConfigSourceLoader
import com.orbitalhq.schema.consumer.SimpleSchemaStore
import com.orbitalhq.schemaServer.core.file.FileChangeDetectionMethod
import com.orbitalhq.schemaServer.core.file.packages.FileSystemPackageLoaderFactory
import com.orbitalhq.schemaServer.core.git.GitProjectSpec
import com.orbitalhq.schemaServer.core.git.GitSchemaPackageLoaderFactory
import com.orbitalhq.schemaServer.core.git.GitWriterDecorator
import com.orbitalhq.schemaServer.core.git.WorkspaceGitProjectConfig
import com.orbitalhq.schemaServer.core.git.packages.BaseGitTest
import com.orbitalhq.schemaServer.core.repositories.InMemoryWorkspaceConfigLoader
import com.orbitalhq.schemaServer.core.repositories.WorkspaceConfig
import com.orbitalhq.schemaServer.core.repositories.WorkspaceProjectsService
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ProjectStoreLifecycleManager
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ReactiveProjectStoreManager
import com.orbitalhq.utils.files.ReactivePollingFileSystemMonitor
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.file.shouldBeAFile
import io.kotest.matchers.file.shouldExist
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import lang.taxi.packages.TaxiProjectLoader
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import reactor.kotlin.test.test
import java.nio.file.Path
import java.time.Duration
import kotlin.io.path.readText
import kotlin.io.path.writeText

class SourceLoaderConnectorsRegistryTest : BaseGitTest() {
   @Rule
   @JvmField
   val tempFolder = TemporaryFolder()
   val defaultPackageIdentifier = PackageIdentifier.fromId("com.test/foo/1.0.0")


   @Before
   fun setup() {
      ConfigFileLocationConventions.resetToDefaults()
   }

   @After
   fun tearDown() {
      ConfigFileLocationConventions.resetToDefaults()
   }

   private fun configFileInTempFolder(resourceName: String): Path {
      return Resources.getResource(resourceName).toURI()
         .copyTo(tempFolder.root)
         .toPath()
   }

   @Test
   fun `can add a connection using a non standard set of config keys`() {
      // Setup...
      val customizedConfigKey = "@flow/config"
      val customizedConfigPath = "flow/config/*.conf"
      ConfigFileLocationConventions.OrbitalConfigKey = customizedConfigKey
      ConfigFileLocationConventions.OrbitalConfigPathEntry = customizedConfigPath
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
            "POSTGRES",
            emptyMap()
         )
      )
      // Ensure we send the warning to the UI
      result.messages.shouldContain(GitWriterDecorator.GIT_COMMIT_NEEDED)

      // Test that the connections file has been created
      val writtenConfigFile = localRepoDir.root.resolve("test-git-repo/flow/config/connections.conf")
      writtenConfigFile.shouldBeAFile()
      writtenConfigFile.shouldExist()


      val taxiConfFile = localRepoDir.root.resolve("test-git-repo/taxi.conf")
      val loadedTaxiConf = TaxiProjectLoader(taxiConfFile.toPath())
         .load()
      loadedTaxiConf.additionalSources.shouldContainKey(customizedConfigKey)
      loadedTaxiConf.additionalSources[customizedConfigKey].shouldBe(customizedConfigPath)
   }

   @Test
   fun `when adding a connection to a taxi project without additional sources declared then the config block is added`() {
      // Create a git project locally
      deployTestProjectToRemoteGitPath(projectName = "sample-project-no-additional-sources")

      // Configure the stack to watch the git repo, and use it as a source
      // for config
      val projectStoreManager = buildProjectStoreManager()
      Awaitility.await().atMost(com.jayway.awaitility.Duration.FIVE_SECONDS).until<Boolean> {
         projectStoreManager.gitLoaders.isNotEmpty()
      }
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
            "POSTGRES",
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
      loadedTaxiConf.additionalSources.shouldContainKey(ConfigFileLocationConventions.DEFAULT_CONFIG_KEY)
      loadedTaxiConf.additionalSources[ConfigFileLocationConventions.DEFAULT_CONFIG_KEY].shouldBe(
         ConfigFileLocationConventions.DEFAULT_CONFIG_PATH
      )
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
            "POSTGRES",
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

   // ORB-624
   @Test
   fun `can add a jdbc connection to a project containing a connections-conf and services-conf`() {
      // Setup...

      // Create a git project locally
      deployTestProjectToRemoteGitPath(projectName = "sample-project-multiple-configs")

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
            "POSTGRES",
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

   @Test
   fun `when connections file edited on disk changes are detected using file watcher and update connections repository`() {
      val configFile = configFileInTempFolder("config/simple-connections.conf")
      val (registry, fileMonitor) = connectionsRegistryForFile(configFile)

      registry.load()
         .jdbc.entries.shouldHaveSize(2)
      registry.load().jdbc.get("another-connection")!!.connectionParameters["host"].shouldBe("our-db-server")


      registry.configUpdated
         .test()
         .expectSubscription()
         .then {
            val updatedConfig = configFile.readText()
               .replace("our-db-server", "our-new-db-server")
            configFile.writeText(updatedConfig)
            fileMonitor.pollNow()
         }
         .expectNextMatches { event ->
            registry.load().jdbc.get("another-connection")!!.connectionParameters["host"].shouldBe("our-new-db-server")
            true
         }
         .thenCancel()
         .verify(Duration.ofSeconds(5))
   }

   // ORB-604
   @Test
   fun `when connections file becomes unhealthy and healthy again changes continue to be detected`() {
      val configFile = configFileInTempFolder("config/simple-connections.conf")
      val (registry, fileMonitor) = connectionsRegistryForFile(configFile)

      registry.load()
         .jdbc.entries.shouldHaveSize(2)
      registry.load().jdbc.get("another-connection")!!.connectionParameters["host"].shouldBe("our-db-server")


      registry.configUpdated
         .test()
         .expectSubscription()
         .then {
            val updatedConfig = configFile.readText()
               .replace("our-db-server", "\${INVALID_ENV_VAR}")
            configFile.writeText(updatedConfig)
            fileMonitor.pollNow()
         }
         .expectNextMatches { event ->
            // Unhealthy
            registry.load().jdbc.shouldBeEmpty()
            registry.configSources.any { it.hasError }.shouldBeTrue()
            true
         }
         .then {
            val updatedConfig = configFile.readText()
               .replace("\${INVALID_ENV_VAR}", "our-new-db-server")
            configFile.writeText(updatedConfig)
            fileMonitor.pollNow()
         }
         .expectNextMatches { event ->
            registry.load().jdbc.get("another-connection")!!.connectionParameters["host"].shouldBe("our-new-db-server")
            true
         }
         .thenCancel()
         .verify(Duration.ofSeconds(5000))
   }

   @Test
   fun `when connections file edited on disk changes are detected using schema watcher and update connections repository`() {
      // Setup
      deployTestProjectToRemoteGitPath()

      // Configure the stack to watch the git repo, and use it as a source
      // for config
      val projectStoreManager = buildProjectStoreManager()
      val gitLoader = projectStoreManager.gitLoaders.single()
      waitForSuccessfulGitClone(projectStoreManager, gitLoader)
      (gitLoader.fileMonitor as ReactivePollingFileSystemMonitor).pollNow()
      projectStoreManager.unhealthyLoaders.shouldBeEmpty()
      val schemaEventSource = SimpleSchemaStore()
      val configSource = ProjectManagerConfigSourceLoader(
         schemaEventSource,
         projectStoreManager,
         filePattern = "connections.conf"
      )
      val connectionsConfFile = gitLoader.workingDir.resolve("orbital/config/connections.conf")
      connectionsConfFile.parent.toFile().mkdirs()
      val connectorRegistry = SourceLoaderConnectorsRegistry(listOf(configSource), listOf(configSource))
      connectorRegistry.configUpdated
         .test()
         .expectSubscription()
         .then {
            connectorRegistry.load().jdbc.shouldBeEmpty()
            connectionsConfFile.writeText(
               """
jdbc {
   my-connection {
      connectionName=my-connection
      connectionParameters {
         database=transactions
         host=our-db-server
         password=super-secret
         port="2003"
         username=jack
      }
      jdbcDriver=POSTGRES
   }
}
            """.trimIndent()
            )
            (gitLoader.fileMonitor as ReactivePollingFileSystemMonitor).pollNow()
            // Trigger a schema changed event
            schemaEventSource.setSchemaSet(schemaEventSource.schemaSet)
         }
         .expectNextMatches { _ ->
            Awaitility.await().atMost(com.jayway.awaitility.Duration.FIVE_SECONDS)
               .until<Boolean> { connectorRegistry.load().jdbc.size == 1 }
            true
         }
         .then {
            // Update the host setting within the file
            connectionsConfFile.writeText(
               """
jdbc {
   my-connection {
      connectionName=my-connection
      connectionParameters {
         database=transactions
         host=our-new-db-server # <---- New host
         password=super-secret
         port="2003"
         username=jack
      }
      jdbcDriver=POSTGRES
   }
}""".trimIndent()
            )
            (gitLoader.fileMonitor as ReactivePollingFileSystemMonitor).pollNow()
            // Trigger a schema changed event
            schemaEventSource.setSchemaSet(schemaEventSource.schemaSet)
         }
         .expectNextMatches {
            Awaitility.await().atMost(com.jayway.awaitility.Duration.FIVE_SECONDS).until<Boolean> {
               connectorRegistry.load().jdbc.values.single().connectionParameters["host"] == "our-new-db-server"
            }
            true
         }
         .thenCancel()
         .verify(Duration.ofSeconds(1000))


   }


   private fun connectionsRegistryForFile(
      configFile: Path,
      packageIdentifier: PackageIdentifier = defaultPackageIdentifier
   ): Pair<SourceLoaderConnectorsRegistry, ReactivePollingFileSystemMonitor> {

      val fileMonitor = ReactivePollingFileSystemMonitor(configFile.parent, Duration.ofDays(999))
      return SourceLoaderConnectorsRegistry(
         listOf(
            FileConfigSourceLoader(
               configFile,
               packageIdentifier = packageIdentifier,
               failIfNotFound = false,
               fileMonitor = fileMonitor
            )
         )
      ) to fileMonitor
   }


   private fun buildProjectStoreManager(): ReactiveProjectStoreManager {
      // Setup: Loading the config from disk
      val eventDispatcher = ProjectStoreLifecycleManager()
//      val loader = FileSchemaRepositoryConfigLoader(configFile.toPath(), eventDispatcher = eventDispatcher)
      val loader = InMemoryWorkspaceConfigLoader(
         WorkspaceConfig(
            git = WorkspaceGitProjectConfig(
               diskChangeDetectionMethod = FileChangeDetectionMethod.POLL,
               diskPollFrequency = Duration.ofDays(1L),
               checkoutRoot = localRepoDir.root.toPath(),
               repositories = listOf(
                  GitProjectSpec(
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
         GitSchemaPackageLoaderFactory(),
         eventDispatcher, eventDispatcher, eventDispatcher
      )

      return repositoryManager
   }
}
