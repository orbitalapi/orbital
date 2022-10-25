package io.vyne.schemaServer.core.git.packages

import com.jayway.awaitility.Awaitility
import com.jayway.awaitility.Awaitility.await
import io.vyne.schemaServer.core.file.FileChangeDetectionMethod
import io.vyne.schemaServer.core.file.deployProject
import io.vyne.schemaServer.core.file.packages.FileSystemPackageLoaderFactory
import io.vyne.schemaServer.core.file.packages.ReactivePollingFileSystemMonitor
import io.vyne.schemaServer.core.git.GitSchemaPackageLoaderFactory
import io.vyne.schemaServer.core.git.GitSchemaRepositoryConfig
import io.vyne.schemaServer.core.publisher.SourceWatchingSchemaPublisher
import io.vyne.schemaServer.core.repositories.FileSchemaRepositoryConfigLoader
import io.vyne.schemaServer.core.repositories.InMemorySchemaRepositoryConfigLoader
import io.vyne.schemaServer.core.repositories.RepositoryService
import io.vyne.schemaServer.core.repositories.SchemaRepositoryConfig
import io.vyne.schemaServer.core.repositories.lifecycle.ReactiveRepositoryManager
import io.vyne.schemaServer.core.repositories.lifecycle.RepositoryLifecycleManager
import io.vyne.schemaServer.repositories.GitRepositoryChangeRequest
import io.vyne.schemaStore.LocalValidatingSchemaStoreClient
import io.vyne.utils.asA
import org.eclipse.jgit.api.Git
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration
import java.util.concurrent.TimeUnit

class GitRepositoryIntegrationTest {

   @Rule
   @JvmField
   val configFolder = TemporaryFolder()

   @Rule
   @JvmField
   val remoteRepoDir = TemporaryFolder()

   @Rule
   @JvmField
   val localRepoDir = TemporaryFolder()
   lateinit var remoteRepo: Git

   @Before
   fun createGitRemote() {
      remoteRepo = Git.init().setDirectory(remoteRepoDir.root).call()
   }

   private fun deployTestProjectToRemoteGitPath(pathInRepository: Path = Paths.get(".")) {
      remoteRepoDir.root.resolve(pathInRepository.toString()).toPath().deployProject("sample-project")
      remoteRepo.add().addFilepattern(".").call()
      remoteRepo.commit().apply { message = "initial" }.call()
   }

   @Test
   fun `configure a git repository at runtime and see initial state pulled along with changes`() {
      deployTestProjectToRemoteGitPath()

      // Setup: Loading the config from disk
      val eventDispatcher = RepositoryLifecycleManager()
//      val loader = FileSchemaRepositoryConfigLoader(configFile.toPath(), eventDispatcher = eventDispatcher)
      val loader = InMemorySchemaRepositoryConfigLoader(
         SchemaRepositoryConfig(
            git = GitSchemaRepositoryConfig(
               checkoutRoot = localRepoDir.root.toPath(),
            )
         ),
         eventDispatcher
      )
      val repositoryService = RepositoryService(loader)

      // Setup: Building the repository manager, which should
      // create new repositories as config is added
      val repositoryManager = ReactiveRepositoryManager(
         FileSystemPackageLoaderFactory(),
         GitSchemaPackageLoaderFactory(
            changeDetectionMethod = FileChangeDetectionMethod.POLL,
            pollFrequency = Duration.ofDays(1)
         ),
         eventDispatcher, eventDispatcher
      )

      // Setup: A SchemaStoreClient, which will
      // compile the taxi as it's discovered / changed
      val schemaClient = LocalValidatingSchemaStoreClient()
      val sourceWatchingSchemaPublisher = SourceWatchingSchemaPublisher(
         schemaClient,
         eventDispatcher
      )

      // Test: Add the git repository
      repositoryService.createGitRepository(
         GitRepositoryChangeRequest(
            "my-git-repo",
            uri = remoteRepoDir.root.toURI().toASCIIString(),
            branch = "master",
         )
      )

      await().atMost(1, TimeUnit.SECONDS)
         .until<Boolean> { repositoryManager.gitLoaders.size == 1 }

      val gitLoader = repositoryManager.gitLoaders.single()

      commitChanges()
      gitLoader.syncNow()
      gitLoader.fileMonitor.asA<ReactivePollingFileSystemMonitor>().pollNow()

      await().atMost(1, TimeUnit.SECONDS)
         .until<Boolean> { schemaClient.schema().hasType("HelloWorld") }
   }

   private fun commitChanges() {
      // commit some changes at the remote
      val file = remoteRepoDir.root.resolve("src/hello.taxi")
      file.writeText("type HelloWorld inherits String")
      remoteRepo.add().addFilepattern("src/hello.taxi").call()
      remoteRepo.commit().apply { message = "update" }.call()
   }
}
