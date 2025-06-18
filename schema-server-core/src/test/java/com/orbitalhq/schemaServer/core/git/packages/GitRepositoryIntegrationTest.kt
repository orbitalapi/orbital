package com.orbitalhq.schemaServer.core.git.packages

import com.jayway.awaitility.Awaitility.await
import com.orbitalhq.schemaServer.core.file.FileChangeDetectionMethod
import com.orbitalhq.schemaServer.core.file.deployProject
import com.orbitalhq.schemaServer.core.file.packages.FileSystemPackageLoaderFactory
import com.orbitalhq.schemaServer.core.git.GitSchemaPackageLoaderFactory
import com.orbitalhq.schemaServer.core.git.WorkspaceGitProjectConfig
import com.orbitalhq.schemaServer.core.publisher.SourceWatchingSchemaPublisher
import com.orbitalhq.schemaServer.core.repositories.InMemoryWorkspaceConfigLoader
import com.orbitalhq.schemaServer.core.repositories.WorkspaceConfig
import com.orbitalhq.schemaServer.core.repositories.WorkspaceProjectsService
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ProjectStoreLifecycleManager
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ReactiveProjectStoreManager
import com.orbitalhq.schemaServer.repositories.git.GitProjectStoreChangeRequest
import com.orbitalhq.schemaStore.LocalValidatingSchemaStoreClient
import com.orbitalhq.utils.files.ReactivePollingFileSystemMonitor
import com.orbitalhq.utils.log
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import lang.taxi.asA
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import reactor.test.StepVerifier
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.io.path.writeText
import kotlin.math.log

class GitRepositoryIntegrationTest : BaseGitTest() {

   /**
    * This is the 2nd remote repo
    */
   @Rule
   @JvmField
   val remoteRepo2Dir = TemporaryFolder()

   /**
    * This test covers a real-world scenario where there are multiple git
    * loaders present.
    *
    * One can become unhealthy (because of a trigger on the file system while another is loading)
    * however that should not cause a cascading failure into other loaders (which is what we've seen)
    */
   @Test
   fun `one unhealthy git repo should not prevent loading of another`() {
      // First, create a remote repo that doesn't contain a taxi.conf file
      remoteRepoDir.root.deployProject("sample-project")
      remoteRepoDir.root.resolve("taxi.conf").delete()
      remoteRepo.add().addFilepattern(".").call()
      remoteRepo.commit().apply { message = "initial" }.call()

      val (eventDispatcher, workspaceProjectsService) = createWorkspaceProjectService()
      val (repositoryManager, schemaClient) = createProjectManager(eventDispatcher)

      // Test: Add the git repository
      workspaceProjectsService.createGitProjectStore(
         GitProjectStoreChangeRequest(
            "my-git-repo",
            uri = remoteRepoDir.root.toURI().toASCIIString(),
            branch = "master",
         )
      )

      StepVerifier
         .create(eventDispatcher.gitSpecAdded)
         .expectNextMatches { gitSpecAddedEvent ->
            gitSpecAddedEvent.spec.name == "my-git-repo"
         }.then {
            await().atMost(1, TimeUnit.SECONDS).until {
               repositoryManager.unhealthyLoaders.isNotEmpty()
            }
         }.then {
            // Now add a second repo.
            // This is the test.
            // We've seen this throw unrecoverable errors, leaving the app in
            // a broken state
            // This fails even though this project is valid, because
            // the earlier project can't be iterated
            val remoteRepo2Dir = Files.createTempDirectory("remote-repo-2")
            val repository2 = FileRepositoryBuilder.create(remoteRepo2Dir.resolve(".git").toFile())
            repository2.create()
            val remoteRepo2 = Git(repository2)
            remoteRepo2Dir.deployProject("sample-project-2")
            remoteRepo2.add().addFilepattern(".").call()
            remoteRepo2.commit().apply { message = "initial" }.call()

            workspaceProjectsService.createGitProjectStore(
               GitProjectStoreChangeRequest(
                  "my-git-repo-2",
                  uri = remoteRepo2Dir.toUri().toASCIIString(),
                  branch = "master",
               )
            )
         }.expectNextMatches { gitSpecAddedEvent ->
            gitSpecAddedEvent.spec.name == "my-git-repo-2"
         }
         .then {
            await().atMost(1, TimeUnit.SECONDS).until<Boolean> {
               repositoryManager.unhealthyLoaders.size == 1 && repositoryManager.gitLoaders.size == 1
            }
         }
         .thenCancel()
         .verify()
   }

   @Test
   fun `if working copy cannot be updated because of local changes then loader becomes unhealthy`() {
      deployTestProjectToRemoteGitPath()

      val (eventDispatcher, workspaceProjectsService) = createWorkspaceProjectService()
      val (repositoryManager, schemaClient) = createProjectManager(eventDispatcher)

      // Test: Add the git repository
      workspaceProjectsService.createGitProjectStore(
         GitProjectStoreChangeRequest(
            "my-git-repo",
            uri = remoteRepoDir.root.toURI().toASCIIString(),
            branch = "master",
         )
      )

      StepVerifier
         .create(eventDispatcher.gitSpecAdded)
         .expectNextMatches { gitSpecAddedEvent ->
            gitSpecAddedEvent.spec.name == "my-git-repo"
         }.verifyTimeout(Duration.ofSeconds(1))

      await().atMost(1, TimeUnit.SECONDS)
         .until<Boolean> { repositoryManager.gitLoaders.size == 1 }

      val gitLoader = repositoryManager.gitLoaders.single()
      gitLoader.syncNow()

      // Modify a file in the working dir, so that future syncs will break
      gitLoader.workingDir.resolve("src/hello.taxi")
         .writeText("// This will prevent a git pull")
      // Make similar changes on the remote to the same file
      commitChanges()

      val syncResult = gitLoader.syncNow()
      syncResult.isClean.shouldBeFalse()
      syncResult.behindCount.shouldBe(1)

      repositoryManager.unhealthyLoaders.shouldHaveSize(1)

   }

   // ORB-972
   @Test
   fun `changes made in a mixed sources repository are detected`() {
      deployTestProjectToRemoteGitPath(projectName = "mixed-sources/avro-and-openapi")
      val (eventDispatcher, workspaceProjectsService) = createWorkspaceProjectService(FileChangeDetectionMethod.POLL)
      val (repositoryManager, schemaClient) = createProjectManager(eventDispatcher)

      // Test: Add the git repository
      workspaceProjectsService.createGitProjectStore(
         GitProjectStoreChangeRequest(
            "my-git-repo",
            uri = remoteRepoDir.root.toURI().toASCIIString(),
            branch = "master",
         )
      )
      await().atMost(30, TimeUnit.SECONDS)
         .until<Boolean> {
            schemaClient.schema().hasType("movies.Film")
         }


      // baseline - verify our field we're about to remove is present
      schemaClient.schema().type("movies.Film").hasAttribute("year").shouldBeTrue()
      val gitLoader = repositoryManager.gitLoaders.single()
      // commit some changes at the remote
      val file = remoteRepoDir.root.resolve("avro/films.avsc")
      file.writeText(
         """{
   "type": "record",
   "name": "Film",
   "taxi.dataType": "movies.Film",
   "namespace": "com.example",
   "fields": [
      {
         "name": "title",
         "type": "string",
         "taxi.dataType": "movies.FilmTitle"
      }
   ]
}"""
      )
      remoteRepo.add().addFilepattern("avro/films.avsc").call()
      remoteRepo.commit().apply { message = "update" }.call()
      log().info("Pushed updates to avro file")

      gitLoader.syncNow()
      gitLoader.fileMonitor.asA<ReactivePollingFileSystemMonitor>().pollNow()

      await().atMost(30, TimeUnit.SECONDS)
         .until<Boolean> {
            val filmType = schemaClient.schema()
               .type("movies.Film")
            // we removed this attribute, so should not be present
            filmType.hasAttribute("year") == false
         }
   }

   @Test
   fun `can add non-taxi sources after a git repo has already been added`() {
      deployTestProjectToRemoteGitPath()
      val (eventDispatcher, workspaceProjectsService) = createWorkspaceProjectService(FileChangeDetectionMethod.POLL)
      val (repositoryManager, schemaClient) = createProjectManager(eventDispatcher)
      commitChanges()
      // Test: Add the git repository
      workspaceProjectsService.createGitProjectStore(
         GitProjectStoreChangeRequest(
            "my-git-repo",
            uri = remoteRepoDir.root.toURI().toASCIIString(),
            branch = "master",
         )
      )
      await().atMost(30, TimeUnit.SECONDS)
         .until<Boolean> { schemaClient.schema().hasType("HelloWorld") }


      // Now, modify the project - first, update the taxi.conf to add
      writeAndCommit("taxi.conf", """
name: taxi/sample
version: 0.3.0
sourceRoot: src/
additionalSources: {
   "@orbital/avro" = "avro/*.avsc"
}
      """.trimIndent())
      // Now add a taxi file with the required defs:
      writeAndCommit("src/films.taxi", """
namespace movies

type FilmTitle inherits String
      """.trimIndent())
      // And add the avro file
      writeAndCommit("avro/films.avsc","""
{
   "type": "record",
   "name": "Film",
   "taxi.dataType": "movies.Film",
   "namespace": "com.example",
   "fields": [
      {
         "name": "title",
         "type": "string",
         "taxi.dataType": "movies.FilmTitle"
      }
   ]
}
      """.trimIndent())

      val gitLoader = repositoryManager.gitLoaders.single()
      log().info("Pushed updates to avro file")

      gitLoader.syncNow()
      gitLoader.fileMonitor.asA<ReactivePollingFileSystemMonitor>().pollNow()

      await().atMost(30, TimeUnit.SECONDS)
         .until<Boolean> {
            val filmType = schemaClient.schema()
               .type("movies.Film")
            // we removed this attribute, so should not be present
            filmType.hasAttribute("title")
         }
   }

   @Test
   fun `configure a git repository at runtime and see initial state pulled along with changes`() {
      deployTestProjectToRemoteGitPath()

      val (eventDispatcher, workspaceProjectsService) = createWorkspaceProjectService(FileChangeDetectionMethod.POLL)
      val (repositoryManager, schemaClient) = createProjectManager(eventDispatcher)

      // Test: Add the git repository
      workspaceProjectsService.createGitProjectStore(
         GitProjectStoreChangeRequest(
            "my-git-repo",
            uri = remoteRepoDir.root.toURI().toASCIIString(),
            branch = "master",
         )
      )

      StepVerifier
         .create(eventDispatcher.gitSpecAdded)
         .expectNextMatches { gitSpecAddedEvent ->
            gitSpecAddedEvent.spec.name == "my-git-repo"
         }.verifyTimeout(Duration.ofSeconds(1))

      await().atMost(30, TimeUnit.SECONDS)
         .until<Boolean> { repositoryManager.gitLoaders.size == 1 }

      val gitLoader = repositoryManager.gitLoaders.single()

      commitChanges()
      gitLoader.syncNow()
      gitLoader.fileMonitor.asA<ReactivePollingFileSystemMonitor>().pollNow()

      await().atMost(30, TimeUnit.SECONDS)
         .until<Boolean> { schemaClient.schema().hasType("HelloWorld") }
   }

   private fun createProjectManager(eventDispatcher: ProjectStoreLifecycleManager): Pair<ReactiveProjectStoreManager, LocalValidatingSchemaStoreClient> {
      // Setup: Building the repository manager, which should
      // create new repositories as config is added
      val repositoryManager = ReactiveProjectStoreManager(
         FileSystemPackageLoaderFactory(),
         GitSchemaPackageLoaderFactory(
         ),
         eventDispatcher, eventDispatcher, eventDispatcher
      )

      // Setup: A SchemaStoreClient, which will
      // compile the taxi as it's discovered / changed
      val schemaClient = LocalValidatingSchemaStoreClient()
      val sourceWatchingSchemaPublisher = SourceWatchingSchemaPublisher(
         schemaClient,
         eventDispatcher
      )
      return Pair(repositoryManager, schemaClient)
   }

   private fun createWorkspaceProjectService(gitDiskChangeDetectionMethod: FileChangeDetectionMethod = FileChangeDetectionMethod.WATCH): Pair<ProjectStoreLifecycleManager, WorkspaceProjectsService> {
      // Setup: Loading the config from disk
      val eventDispatcher = ProjectStoreLifecycleManager()
      //      val loader = FileSchemaRepositoryConfigLoader(configFile.toPath(), eventDispatcher = eventDispatcher)
      val loader = InMemoryWorkspaceConfigLoader(
         WorkspaceConfig(
            git = WorkspaceGitProjectConfig(
               checkoutRoot = localRepoDir.root.toPath(),
               diskChangeDetectionMethod = gitDiskChangeDetectionMethod
            )
         ),
         eventDispatcher
      )
      val workspaceProjectsService = WorkspaceProjectsService(loader)
      return Pair(eventDispatcher, workspaceProjectsService)
   }

   private fun commitChanges() {
      // commit some changes at the remote
      val file = remoteRepoDir.root.resolve("src/hello.taxi")
      file.writeText("type HelloWorld inherits String")
      remoteRepo.add().addFilepattern("src/hello.taxi").call()
      remoteRepo.commit().apply { message = "update" }.call()
   }

   private fun writeAndCommit(path: String, contents: String) {
      val file = remoteRepoDir.root.resolve(path)
      if (!file.exists()) {
         file.parentFile.mkdirs()
         file.createNewFile()
      }
      file.writeText(contents)
      remoteRepo.add().addFilepattern(path).call()
      remoteRepo.commit().apply { message = "update" }.call()
   }
}
