package io.vyne.schemaServer.changesets

import com.google.common.io.Resources
import com.jayway.awaitility.Awaitility.await
import com.jcraft.jsch.JSch
import com.winterbe.expekt.expect
import io.vyne.PackageIdentifier
import io.vyne.VersionedSource
import io.vyne.schema.consumer.SchemaStore
import io.vyne.schemaServer.core.file.FileChangeDetectionMethod
import io.vyne.schemaServer.core.file.packages.FileSystemPackageLoaderFactory
import io.vyne.schemaServer.core.file.packages.ReactivePollingFileSystemMonitor
import io.vyne.schemaServer.core.git.GitSchemaPackageLoaderFactory
import io.vyne.schemaServer.core.repositories.lifecycle.ReactiveRepositoryManager
import io.vyne.schemaServer.core.repositories.lifecycle.RepositoryLifecycleEventDispatcher
import io.vyne.schemaServer.core.repositories.lifecycle.RepositoryLifecycleEventSource
import io.vyne.schemaServer.core.repositories.lifecycle.RepositorySpecLifecycleEventSource
import io.vyne.schemaServer.editor.AddChangesToChangesetRequest
import io.vyne.schemaServer.editor.FinalizeChangesetRequest
import io.vyne.schemaServer.editor.SchemaEditorApi
import io.vyne.schemaServer.editor.SetActiveChangesetRequest
import io.vyne.schemaServer.editor.StartChangesetRequest
import io.vyne.schemaServer.editor.toFilename
import org.apache.commons.io.FileUtils
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.RefSpec
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.junit.ClassRule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.junit4.SpringRunner
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit


fun URI.copyDirectoryTo(destDirectory: File): File {
   val source = try {
      File(this)
   } catch (e: Exception) {
      throw RuntimeException("Failed to create file from URI ${this.toASCIIString()}", e)
   }

   val destFile = destDirectory.resolve(source.name)
   FileUtils.copyDirectory(source, destFile)
   return destFile
}

const val descriptionUpdate = """namespace film.types {
   [[ Documentation goes here ]]
   type Description inherits String
}"""

@Testcontainers
@RunWith(SpringRunner::class)
@SpringBootTest(properties = ["spring.main.allow-bean-definition-overriding=true"])
class GitChangesetsTest {
   @Autowired
   private lateinit var schemaEditorApi: SchemaEditorApi

   @Autowired
   private lateinit var gitLoaderFactory: GitSchemaPackageLoaderFactory

   @Autowired
   private lateinit var repositoryManager: ReactiveRepositoryManager

   @Autowired
   private lateinit var schemaStore: SchemaStore
   private val packageIdentifier = PackageIdentifier("io.vyne", "films", "0.1.0")

   init {
      // git server connection host key is not known so we want to allow connecting without checking the host key
      JSch.setConfig("StrictHostKeyChecking", "no")
   }

   // Use polling over watching as the repository does not exist at the time of initialization and as such watching fails
   @TestConfiguration
   internal class TestConfig {

      @Bean
      fun gitLoaderFactory() = GitSchemaPackageLoaderFactory(changeDetectionMethod = FileChangeDetectionMethod.POLL)

      @Primary
      @Bean
      fun repositoryManager(
         eventSource: RepositorySpecLifecycleEventSource,
         eventDispatcher: RepositoryLifecycleEventDispatcher,
         gitLoaderFactory: GitSchemaPackageLoaderFactory,
         repositoryEventSource: RepositoryLifecycleEventSource
      ): ReactiveRepositoryManager {
         return ReactiveRepositoryManager(
            FileSystemPackageLoaderFactory(),
            gitLoaderFactory,
            eventSource, eventDispatcher, repositoryEventSource
         )
      }
   }

   @Test
   fun `changesets work with git as expected (create a changeset, add changes into it, change active changeset and finalize changeset)`() {
      expect(schemaStore.schema().sources).to.be.empty
      initializeSchemaToRepo(gitServerContainer, mainTempFolder.newFolder(), gitRepoUri)

      pollForChangesNow()
      await().atMost(10, TimeUnit.SECONDS).until<Boolean> {
         val store = schemaStore
         val schema = store.schema()
         schema.hasType("film.types.Description") && schemaStore.schema()
            .type("film.types.Description").typeDoc?.isEmpty() ?: false
      }

      schemaEditorApi.createChangeset(StartChangesetRequest("test-changes", packageIdentifier)).block()

      val filename = schemaStore.schema().type("film.types.Description").name.toFilename()
      schemaEditorApi.addChangesToChangeset(
         AddChangesToChangesetRequest(
            "test-changes",
            packageIdentifier,
            listOf(VersionedSource.unversioned(filename, descriptionUpdate))
         )
      ).block()

      pollForChangesNow()
      await().atMost(10, TimeUnit.SECONDS).until<Boolean> {
         schemaStore.schema().type("film.types.Description").typeDoc == "Documentation goes here"
      }

      schemaEditorApi.setActiveChangeset(SetActiveChangesetRequest("main", packageIdentifier)).block()

      pollForChangesNow()
      await().atMost(10, TimeUnit.SECONDS).until<Boolean> {
         schemaStore.schema().hasType("film.types.Description") && schemaStore.schema()
            .type("film.types.Description").typeDoc?.isEmpty() ?: false
      }

      schemaEditorApi.setActiveChangeset(SetActiveChangesetRequest("test-changes", packageIdentifier)).block()

      pollForChangesNow()
      await().atMost(10, TimeUnit.SECONDS).until<Boolean> {
         schemaStore.schema().type("film.types.Description").typeDoc == "Documentation goes here"
      }

      schemaEditorApi.finalizeChangeset(FinalizeChangesetRequest("test-changes", packageIdentifier)).block()
   }

   private fun pollForChangesNow() {
      val gitLoader = repositoryManager.gitLoaders.single()
      gitLoader.syncNow()
      val fileMonitor = gitLoader.fileMonitor as ReactivePollingFileSystemMonitor
      fileMonitor.pollNow()

   }



   companion object {
      val gitServerImage = DockerImageName.parse("rockstorm/git-server").withTag("2.38")

      @ClassRule
      @JvmField
      val mainTempFolder = TemporaryFolder()

      private fun configFileInTempFolder(resourceName: String): File {
         val folder = mainTempFolder.newFolder()
         val file = folder.resolve("schema-server.conf")

         val gitFolder = folder.resolve("git").absolutePath
            .replace("\\", "\\\\") // Escape any singular backlashes as they're not valid inside the config file
         val fileContents = Resources.toString(Resources.getResource(resourceName), StandardCharsets.UTF_8)
            .replace(
               "checkoutRoot=\"\"",
               "checkoutRoot=\"$gitFolder\""
            )
            .replace(
               "uri=\"\"",
               "uri=\"$gitRepoUri\""
            )

         val writer = BufferedWriter(FileWriter(file))
         writer.write(fileContents)
         writer.close()

         return file
      }

      val gitRepoUri
         get() = "ssh://git@${gitServerContainer.host}:${gitServerContainer.firstMappedPort}/home/git/test-repo-remote"


      fun initializeGitServer(path: String): GenericContainer<*> {
         return GenericContainer(gitServerImage)
            .withExposedPorts(22)
            .withFileSystemBind(
               path,
               "/home/git/test-repo-remote",
               BindMode.READ_WRITE
            )
            .apply {
               start()
            }
      }

      fun initializeSchemaToRepo(gitContainer:GenericContainer<*>, cloneFolder:File, gitRepoUri: String) {
         // Initialize the git repo by first fixing the permissions of the folder to be used and then creating a bare git repo
         // ash is the sh of alpine on which the image used is based
         gitContainer.execInContainer("ash", "-c", "/bin/chown -hR git:git /home/git/test-repo-remote")
         gitContainer.execInContainer(
            "ash",
            "-c",
            "/home/git/git-shell-commands/git-init --bare --shared=all -b main /home/git/test-repo-remote"
         )

//      val tempCloneFolder = mainTempFolder.newFolder()
         val git = Git.init()
            .setDirectory(cloneFolder)
            .setInitialBranch("main")
            .call()


         Resources.getResource("changesets/projects").toURI().copyDirectoryTo(cloneFolder)

         git.add()
            .addFilepattern(".")
            .call()

         git.commit()
            .setMessage("Initial commit")
            .call()

         git.remoteAdd()
            .setName("origin")
            .setUri(URIish(gitRepoUri))
            .call()

         git.push()
            .setRemote("origin")
            .setPushAll()
            .setRefSpecs(RefSpec("main:main"))
            .setCredentialsProvider(UsernamePasswordCredentialsProvider("git", "12345"))
            .call()
      }

      @JvmStatic
      @DynamicPropertySource
      fun registerDynamicProperties(registry: DynamicPropertyRegistry) {
         gitServerContainer = initializeGitServer(mainTempFolder.newFolder().absolutePath)
         registry.add("vyne.repositories.config-file") { configFileInTempFolder("changesets/schema-server.conf").absolutePath }
      }

      lateinit var gitServerContainer: GenericContainer<*>
   }
}
