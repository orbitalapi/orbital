package io.vyne.schemaServer.core.repositories

import com.jayway.awaitility.Awaitility
import com.winterbe.expekt.should
import io.vyne.PackageIdentifier
import io.vyne.schemaServer.core.file.packages.FileSystemPackageLoaderFactory
import io.vyne.schemaServer.core.git.GitSchemaPackageLoaderFactory
import io.vyne.schemaServer.core.publisher.SourceWatchingSchemaPublisher
import io.vyne.schemaServer.core.repositories.lifecycle.ReactiveRepositoryManager
import io.vyne.schemaServer.core.repositories.lifecycle.RepositoryLifecycleManager
import io.vyne.schemaServer.repositories.CreateFileRepositoryRequest
import io.vyne.schemaStore.LocalValidatingSchemaStoreClient
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.concurrent.TimeUnit

class FileRepositoryIntegrationTest {


   @Rule
   @JvmField
   val folder = TemporaryFolder()

   @Test
   fun `configure a file repository at runtime and when files are changes then schema updates are emitted`() {
      // Setup: Loading the config from disk
      val configFile = folder.root.resolve("repositories.conf")
      val eventDispatcher = RepositoryLifecycleManager()
      val loader = FileSchemaRepositoryConfigLoader(configFile.toPath(), eventDispatcher = eventDispatcher)
      val repositoryService = RepositoryService(loader)

      // Setup: Building the file repository, which should
      // create new repositories as config is added
      val repositoryManager = ReactiveRepositoryManager(
         FileSystemPackageLoaderFactory(),
         GitSchemaPackageLoaderFactory(),
         eventDispatcher, eventDispatcher
      )

      // Setup: A SchemaStoreClient, which will
      // compile the taxi as it's discovered / changed
      val schemaClient = LocalValidatingSchemaStoreClient()
      val sourceWatchingSchemaPublisher = SourceWatchingSchemaPublisher(
         schemaClient,
         eventDispatcher
      )

      // First, create the new repository
      val projectFolder = folder.newFolder("my-project")
      repositoryService.createFileRepository(
         CreateFileRepositoryRequest(
            projectFolder.canonicalPath,
            true,
            PackageIdentifier.fromId("foo/test/0.1.0")
         )
      )

      repositoryManager.fileLoaders.should.have.size(1)
      schemaClient.schema()
         .hasType("Hello")
         .should.be.`false`


      projectFolder.resolve("src/hello.taxi")
         .writeText("""type Hello inherits String""")

      Awaitility.await()
         .atMost(2, TimeUnit.SECONDS)
         .until<Boolean> {
            schemaClient.schema()
               .hasType("Hello")
         }

   }

   @Test
   fun `on startup existing repositories are compiled`() {
      // Setup: Loading the config from disk
      val configFile = folder.root.resolve("repositories.conf")
      val setupLoader =
         FileSchemaRepositoryConfigLoader(configFile.toPath(), eventDispatcher = RepositoryLifecycleManager())
      val setupRepositoryService = RepositoryService(setupLoader)

      // First, create the project, and write some source.
      val projectFolder = folder.newFolder("my-project")
      setupRepositoryService.createFileRepository(
         CreateFileRepositoryRequest(
            projectFolder.canonicalPath,
            true,
            PackageIdentifier.fromId("foo/test/0.1.0")
         )
      )
      projectFolder.resolve("src/hello.taxi")
         .writeText("""type Hello inherits String""")


      // Now, "restart", by creating a new set of components.
      val eventDispatcher = RepositoryLifecycleManager()
      val loader = FileSchemaRepositoryConfigLoader(configFile.toPath(), eventDispatcher = eventDispatcher)

      // Setup: Building the file repository, which should
      // create new repositories as config is added
      val repositoryManager = ReactiveRepositoryManager(
         FileSystemPackageLoaderFactory(),
         GitSchemaPackageLoaderFactory(),
         eventDispatcher, eventDispatcher
      )

      // Setup: A SchemaStoreClient, which will
      // compile the taxi as it's discovered / changed
      val schemaClient = LocalValidatingSchemaStoreClient()
      val sourceWatchingSchemaPublisher = SourceWatchingSchemaPublisher(
         schemaClient,
         eventDispatcher
      )

      Awaitility.await()
         .atMost(2, TimeUnit.SECONDS)
         .until<Boolean> {
            schemaClient.schema()
               .hasType("Hello")
         }


   }
}
