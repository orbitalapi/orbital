package com.orbitalhq.schemaServer.core.repositories

import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.io.Resources
import com.jayway.awaitility.Awaitility
import com.nhaarman.mockito_kotlin.mock
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.schema.api.SchemaSet
import com.orbitalhq.schema.rsocket.CBORJackson
import com.orbitalhq.schemaServer.core.file.FileProjectSpec
import com.orbitalhq.schemaServer.core.file.packages.FileSystemPackageLoaderFactory
import com.orbitalhq.schemaServer.core.git.GitProjectSpec
import com.orbitalhq.schemaServer.core.git.GitSchemaPackageLoaderFactory
import com.orbitalhq.schemaServer.core.publisher.SourceWatchingSchemaPublisher
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ProjectStoreLifecycleManager
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ReactiveProjectStoreManager
import com.orbitalhq.schemaServer.packages.AvroPackageLoaderSpec
import com.orbitalhq.schemaServer.packages.OpenApiPackageLoaderSpec
import com.orbitalhq.schemaServer.packages.SoapPackageLoaderSpec
import com.orbitalhq.schemaServer.packages.TaxiPackageLoaderSpec
import com.orbitalhq.schemaServer.repositories.AddFileProjectRequest
import com.orbitalhq.schemaStore.LocalValidatingSchemaStoreClient
import com.orbitalhq.schemaStore.TaxiSchemaValidator
import com.orbitalhq.schemas.readers.TaxiSourceConverter
import com.orbitalhq.test.utils.FlakeyOnBuildServer
import com.winterbe.expekt.should
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.file.shouldExist
import io.kotest.matchers.file.shouldNotBeEmpty
import io.kotest.matchers.maps.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import lang.taxi.packages.SourcesTypes
import lang.taxi.packages.TaxiPackageLoader
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import reactor.kotlin.core.publisher.toFlux
import reactor.kotlin.test.test
import java.io.File
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.absolutePathString
import kotlin.io.path.toPath
import kotlin.test.assertFailsWith

class FileRepositoryIntegrationTest {
   @TempDir
   lateinit var folder: File

   @Test
   fun `adding a file repository to an empty folder creates a taxi project`() {
      val configFile = folder.resolve("workspace.conf")
      val eventDispatcher = ProjectStoreLifecycleManager()
      val loader =
         FileWorkspaceConfigLoader(configFile.toPath(), eventDispatcher = eventDispatcher, projectManager = mock { })

      val projectFolder = folder.newFolder().toPath()
      loader.addFileSpec(
         FileProjectSpec(
            path = projectFolder,
            packageIdentifier = PackageIdentifier.fromId("com/foo/1.0.0")
         )
      )

      val createdProject = TaxiPackageLoader.forDirectoryContainingTaxiFile(projectFolder).load()
      createdProject.identifier.id.shouldBe("com/foo/1.0.0")
   }


   @Test
   fun `can add an openApi spec`() {
      // Copy the OpenAPI spec somewhhere
      val openApiSpec = folder.newFile()
      Resources.copy(Resources.getResource("open-api/petstore-expanded.yaml"), openApiSpec.outputStream())

      val configFile = folder.resolve("workspace.conf")
      val eventDispatcher = ProjectStoreLifecycleManager()
      val loader =
         FileWorkspaceConfigLoader(configFile.toPath(), eventDispatcher = eventDispatcher, projectManager = mock { })

      loader.addFileSpec(
         FileProjectSpec(
            path = openApiSpec.toPath(),
            loader = OpenApiPackageLoaderSpec(
               identifier = PackageIdentifier.fromId("com/foo/1.0.0"),
               defaultNamespace = "com.foo"
            )
         )
      )
   }

   @Test
   fun `can add a soap spec`() {
      val (repositoryService, repositoryManager, schemaClient) = setupServices(folder)

      repositoryManager.use {
         // First, create the new repository
         val projectFolder = folder.newFolder()
         val targetFile = projectFolder.resolve("src/country-info.wsdl")
         targetFile.parentFile.mkdirs()
         targetFile.createNewFile()
         Resources.copy(Resources.getResource("soap/TrimmedCountryInfoServiceSpec.wsdl"), targetFile.outputStream())


         repositoryService.createFileRepository(
            AddFileProjectRequest(
               projectFolder.canonicalPath,
               true,
               loader = SoapPackageLoaderSpec(
                  PackageIdentifier.fromId("com/foo/1.0.0")
               ),
               newProjectIdentifier = PackageIdentifier.fromId("com/foo/1.0.0")
            )
         )

         Awaitility.await().atMost(10, TimeUnit.SECONDS)
            .until<Boolean> {
               repositoryManager.fileLoaders.size == 1
            }
         repositoryManager.fileLoaders.should.have.size(1)
         schemaClient.schema()
            .hasType("Hello")
            .should.be.`false`

         Awaitility.await()
            .atMost(10, TimeUnit.SECONDS)
            .until<Boolean> {
               schemaClient.schema()
                  .services.isNotEmpty()
            }
         val schema = schemaClient.schema()
         schema.services.shouldHaveSize(1)
         schema.additionalSources.shouldNotBeEmpty()
         schema.additionalSources.containsKey(SourcesTypes.SOURCE_MAP)
            .shouldBeTrue()
         schema.additionalSources.containsKey(SourcesTypes.ORIGINAL_SOURCE)
            .shouldBeTrue()
      }
   }

   @Test
   fun `can add an avro spec as a workspace project`() {
      val (workspaceProjectsService, reactiveProjectStoreManager, schemaClient) = setupServices(folder)

      reactiveProjectStoreManager.use {
         // First, create the new repository
         val projectFolder = folder.newFolder()
         val targetFile = projectFolder.resolve("src/addressBook.avsc")
         targetFile.parentFile.mkdirs()
         targetFile.createNewFile()
         Resources.copy(Resources.getResource("avro/addressBook.avsc"), targetFile.outputStream())

         workspaceProjectsService.createFileRepository(
            AddFileProjectRequest(
               projectFolder.canonicalPath,
               true,
               loader = AvroPackageLoaderSpec(
                  PackageIdentifier.fromId("com/foo/1.0.0")
               ),
               newProjectIdentifier = PackageIdentifier.fromId("com/foo/1.0.0")
            )
         )

         // This assertion made the test flakey.
//         reactiveProjectStoreManager.fileLoaders.should.have.size(1)
         Awaitility.await()
            .atMost(10, TimeUnit.SECONDS)
            .until<Boolean> {
               // Should have the type defined in the avro schema
               schemaClient.schema()
                  .hasType("simple.AddressBook")
            }
      }
   }

   @Test
   @FlakeyOnBuildServer
   fun `configure a file repository at runtime and when files are changes then schema updates are emitted`() {
      val (repositoryService, repositoryManager, schemaClient) = setupServices(folder)


      repositoryManager.use {
         // First, create the new repository
         val projectFolder = folder.newFolder()
         repositoryService.createFileRepository(
            AddFileProjectRequest(
               projectFolder.canonicalPath,
               true,
               loader = TaxiPackageLoaderSpec,
               newProjectIdentifier = PackageIdentifier.fromId("com/foo/1.0.0")
            )
         )

         Awaitility.await()
            .atMost(5, TimeUnit.SECONDS)
            .until<Boolean> { repositoryManager.fileLoaders.size == 1 }
         repositoryManager.fileLoaders.should.have.size(1)
         schemaClient.schema()
            .hasType("Hello")
            .should.be.`false`


         projectFolder
            .resolve("src/hello.taxi")
            .writeText("""type Hello inherits String""")

         Awaitility.await()
            .atMost(90, TimeUnit.SECONDS)
            .until<Boolean> {
               schemaClient.schema()
                  .hasType("Hello")
            }
      }
   }

   @Test
   @Disabled("Test passes on its own but not when run with others - cannot work out why")
   fun `after removing project changes to sources in the removed project do not trigger schema updates`() {
      val (repositoryService, repositoryManager, schemaClient) = setupServices(folder)

      repositoryManager.use {
         // First, create the new repository
         val projectFolder = folder.newFolder()
         val packageIdentifier = PackageIdentifier.fromId("com/foo/1.0.0")
         repositoryService.createFileRepository(
            AddFileProjectRequest(
               projectFolder.canonicalPath,
               true,
               loader = TaxiPackageLoaderSpec,
               newProjectIdentifier = packageIdentifier
            )
         )

         repositoryManager.fileLoaders.should.have.size(1)
         schemaClient.schema()
            .hasType("Hello")
            .should.be.`false`

         schemaClient.schemaChanged.toFlux()
            .test()
            .expectSubscription()
            .expectNextMatches {
               // init event, ignore this one
               it.newSchemaSet.generation == 1
            }
            .then {
               projectFolder
                  .resolve("src/hello.taxi")
                  .writeText("""type Hello inherits String""")
            }
            .expectNextMatches { event ->
               event.newSchemaSet.schema.hasType("Hello").shouldBeTrue()
               true
            }
            .then {
               // Now, remove the project
               repositoryService.removeFileRepository(projectFolder.toPath(), packageIdentifier)
            }
            .expectNextMatches { event ->
               event.newSchemaSet.schema.hasType("Hello").shouldBeFalse()
               true
            }.then {
               // Now change a file in the removed repository
               projectFolder
                  .resolve("src/world.taxi")
                  .writeText("""type World inherits String""")
            }
            .expectNoEvent(Duration.ofSeconds(3))
            .thenCancel()
            .verify()
      }
   }


   @Test
   @Disabled("Test passes on its own but not when run with others - cannot work out why")
   fun `after removing project by editing workspace conf file then schema is updated`() {
      val (workspaceService, repositoryManager, schemaClient, loader) = setupServices(folder)
      repositoryManager.use {
         // First, create the new repository
         val projectFolder = folder.newFolder()
         val packageIdentifier = PackageIdentifier.fromId("com/foo/1.0.0")
         workspaceService.createFileRepository(
            AddFileProjectRequest(
               projectFolder.canonicalPath,
               true,
               loader = TaxiPackageLoaderSpec,
               newProjectIdentifier = packageIdentifier,

               )
         )

         repositoryManager.fileLoaders.should.have.size(1)
         schemaClient.schema()
            .hasType("Hello")
            .should.be.`false`

         Thread.sleep(3000)

         schemaClient.schemaChanged.toFlux()
            .test()
            .expectSubscription()
            .expectNextMatches {
               // init event, ignore this one
               if (repositoryManager.fileLoaders.size > 1) {
                  println()
               }
               repositoryManager.fileLoaders.should.have.size(1)
               true
            }
            .then {
               projectFolder
                  .resolve("src/hello.taxi")
                  .writeText("""type Hello inherits String""")
            }
            .expectNextMatches { event ->
               event.newSchemaSet.schema.hasType("Hello").shouldBeTrue()
               true
            }
            .then {
               val currentState = loader.load()
               // Now, remove the project.
               // We'll edit the file directly, change watchers should pick it up.

               val workspaceFile = loader.configFilePath.toFile()
               workspaceFile.shouldExist()
               workspaceFile.shouldNotBeEmpty()
               // Remove the file repository
               workspaceFile.writeText("file {}")
               loader.filePathChanged(workspaceFile.toPath())
            }
            .expectNextMatches { event ->
               event.newSchemaSet.schema.hasType("Hello").shouldBeFalse()
               true
            }.then {
               // Now change a file in the removed repository
               projectFolder
                  .resolve("src/world.taxi")
                  .writeText("""type World inherits String""")
            }
            .expectNoEvent(Duration.ofSeconds(3))
            .thenCancel()
            .verify()
      }
   }

   data class TestServices(
      val a: WorkspaceProjectsService,
      val b: ReactiveProjectStoreManager,
      val c: LocalValidatingSchemaStoreClient,
      val d: FileWorkspaceConfigLoader
   )

   companion object {
      fun setupServices(rootFolder: File): TestServices {
         // Setup: Loading the config from disk
         val configFile = rootFolder.newFolder().resolve("workspace.conf")
         val eventDispatcher = ProjectStoreLifecycleManager()

         // Setup: Building the file repository, which should
         // create new repositories as config is added
         val repositoryManager = ReactiveProjectStoreManager(
            FileSystemPackageLoaderFactory(),
            GitSchemaPackageLoaderFactory(),
            eventDispatcher, eventDispatcher, eventDispatcher
         )

         val loader = FileWorkspaceConfigLoader(
            configFile.toPath(),
            eventDispatcher = eventDispatcher,
            projectManager = repositoryManager
         )
         val workspaceProjectsService = WorkspaceProjectsService(loader)


         // Setup: A SchemaStoreClient, which will
         // compile the taxi as it's discovered / changed
         val schemaClient = LocalValidatingSchemaStoreClient(
            schemaValidator = TaxiSchemaValidator(
               listOf(
                  TaxiSourceConverter,
               )
            )
         )
         val sourceWatchingSchemaPublisher = SourceWatchingSchemaPublisher(
            schemaClient,
            eventDispatcher
         )
         return TestServices(workspaceProjectsService, repositoryManager, schemaClient, loader)
      }


   }

   @Test
   fun `declaring projects that contain additional sources are loaded into the schema`() {
      // Setup: Loading the config from disk
      val configFile = folder.newFolder().resolve("workspace.conf")
      val eventDispatcher = ProjectStoreLifecycleManager()
      val loader =
         FileWorkspaceConfigLoader(configFile.toPath(), eventDispatcher = eventDispatcher, projectManager = mock { })
      val workspaceProjectsService = WorkspaceProjectsService(loader)

      // Setup: Building the file repository, which should
      // create new repositories as config is added
      val repositoryManager = ReactiveProjectStoreManager(
         FileSystemPackageLoaderFactory(),
         GitSchemaPackageLoaderFactory(),
         eventDispatcher, eventDispatcher, eventDispatcher
      )

      repositoryManager.use {
         // Setup: A SchemaStoreClient, which will
         // compile the taxi as it's discovered / changed
         val schemaClient = LocalValidatingSchemaStoreClient()
         val sourceWatchingSchemaPublisher = SourceWatchingSchemaPublisher(
            schemaClient,
            eventDispatcher
         )

         val path = Resources.getResource("additional-sources").toURI().toPath()
         workspaceProjectsService.createFileRepository(
            AddFileProjectRequest(
               path = path.absolutePathString(),
               isEditable = false
            )
         ).block()

         Awaitility.await()
            .atMost(2, TimeUnit.HOURS)
            .until<Boolean> {
               schemaClient.schema()
                  .hasType("Hello")
            }

         val loadedSources = schemaClient.schema().additionalSources
         loadedSources.entries.shouldHaveSize(1)
         loadedSources["@orbital/pipelines"]!!.shouldHaveSize(1)
         val loadedPipelines = loadedSources["@orbital/pipelines"]!!.single().sources
         loadedPipelines.shouldHaveSize(1)

         // Can we load this to/from CBOR (for sending over rsocket)?
         val schema = schemaClient.schema()
         val schemaSet = SchemaSet.from(schema, 1)

         val bytes = CBORJackson.defaultMapper.writeValueAsBytes(schemaSet)
         val deserialized = CBORJackson.defaultMapper.readValue<SchemaSet>(bytes)
         deserialized.schema.additionalSources.shouldBe(schemaClient.schema().additionalSources)
      }
   }

   @Test
   fun `on startup existing repositories are compiled`() {
      // Setup: Loading the config from disk
      val configFile = folder.newFolder().resolve("workspace.conf")
      val setupLoader =
         FileWorkspaceConfigLoader(
            configFile.toPath(),
            eventDispatcher = ProjectStoreLifecycleManager(),
            projectManager = mock { })
      val setupWorkspaceProjectsService = WorkspaceProjectsService(setupLoader)

      // First, create the project, and write some source.
      val projectFolder = folder.newFolder("my-project")
      setupWorkspaceProjectsService.createFileRepository(
         AddFileProjectRequest(
            projectFolder.canonicalPath,
            true,
            loader = TaxiPackageLoaderSpec,
            newProjectIdentifier = PackageIdentifier.fromId("com/foo/1.0.0")
         )
      )
      projectFolder.resolve("src/hello.taxi")
         .writeText("""type Hello inherits String""")


      // Now, "restart", by creating a new set of components.
      val eventDispatcher = ProjectStoreLifecycleManager()
      val loader =
         FileWorkspaceConfigLoader(configFile.toPath(), eventDispatcher = eventDispatcher, projectManager = mock { })

      // Setup: Building the file repository, which should
      // create new repositories as config is added
      ReactiveProjectStoreManager(
         FileSystemPackageLoaderFactory(),
         GitSchemaPackageLoaderFactory(),
         eventDispatcher, eventDispatcher, eventDispatcher
      ).use {
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


   @Test
   fun `can delete git repository`() {
      val configFile = folder.newFolder().resolve("workspace.conf")
      val schemaRepository =
         FileWorkspaceConfigLoader(
            configFile.toPath(),
            eventDispatcher = ProjectStoreLifecycleManager(),
            projectManager = mock { })

      schemaRepository.load()
         .git?.repositories?.should?.be?.empty

      createFourRepositories(schemaRepository)
      schemaRepository.shouldHaveRepositories(fileRepoCount = 2, gitRepoCount = 2)

      schemaRepository.removeGitRepository("test-repo-1", PackageIdentifier.fromId("foo/bar/1.2"))

      schemaRepository.shouldHaveRepositories(fileRepoCount = 2, gitRepoCount = 1)
   }

   @Test
   fun `can delete file repository`() {
      val configFile = folder.newFolder().resolve("workspace.conf")
      val schemaRepository =
         FileWorkspaceConfigLoader(
            configFile.toPath(),
            eventDispatcher = ProjectStoreLifecycleManager(),
            projectManager = mock { })

      schemaRepository.load()
         .git?.repositories?.should?.be?.empty

      createFourRepositories(schemaRepository)
      schemaRepository.shouldHaveRepositories(fileRepoCount = 2, gitRepoCount = 2)

      schemaRepository.removeFileRepository(configFile.toPath(), PackageIdentifier.fromId("com/foo/1.0.0"))

      schemaRepository.shouldHaveRepositories(fileRepoCount = 1, gitRepoCount = 2)
   }

   @Test
   fun `can add and remove repository concurrently`() {
      // This test relates to ORB-496, where it was observed that somehow
      // the workspace.conf file got corrupted.
      // The goal of this test is that when concurrent users are adding / removing
      // the file doesn't become corrupted.
      // We're not testing that concurrent writes detect conflicts - just that the file
      // doesn't get corrupted.
      val configFile = folder.newFolder().resolve("workspace.conf")

      val schemaRepository =
         FileWorkspaceConfigLoader(
            configFile.toPath(),
            eventDispatcher = ProjectStoreLifecycleManager(),
            projectManager = mock { })

      val parseFailed = AtomicBoolean(false)
      fun invalidateCacheAndReload() {
         schemaRepository.invalidateCache()
         try {
            schemaRepository.load()
         } catch (e: Exception) {
            parseFailed.set(true)
         }
      }

      val workerThreadOne = Thread {
         repeat(100) {
            schemaRepository.addFileSpec(
               FileProjectSpec(
                  folder.resolve("project-1/").toPath(),
                  loader = TaxiPackageLoaderSpec,
                  packageIdentifier = PackageIdentifier.fromId("com/foo/1.0.0")
               )
            )
            schemaRepository.removeFileRepository(
               folder.resolve("project-1/").toPath(),
               packageIdentifier = PackageIdentifier.fromId("com/foo/1.0.0")
            )
            invalidateCacheAndReload()

         }
      }

      val workerThreadTwo = Thread {
         repeat(100) {
            schemaRepository.addFileSpec(
               FileProjectSpec(
                  folder.resolve("project-1/").toPath(),
                  loader = TaxiPackageLoaderSpec,
                  packageIdentifier = PackageIdentifier.fromId("com/foo/1.0.0")
               )
            )
            schemaRepository.removeFileRepository(
               folder.resolve("project-1/").toPath(),
               packageIdentifier = PackageIdentifier.fromId("com/foo/1.0.0")
            )
            invalidateCacheAndReload()
         }
      }

      val workerThreadThree = Thread {
         repeat(100) {
            schemaRepository.addGitSpec(
               GitProjectSpec(
                  "test-repo-2",
                  "https://github.com/test/repo2",
                  "master",
               )
            )
            schemaRepository.removeGitRepository(
               "test-repo-2",
               PackageIdentifier.fromId("com.test/not-used/1.0.0")
            )
            invalidateCacheAndReload()
         }
      }

      workerThreadOne.start()
      workerThreadTwo.start()
      workerThreadThree.start()

      workerThreadOne.join()
      workerThreadTwo.join()
      workerThreadThree.join()

      // The workspace.conf file should still be valid
      invalidateCacheAndReload()
      parseFailed.get().shouldBeFalse()

   }

   @Test
   fun `throws error removing file repository that doesn't exist`() {
      val configFile = folder.newFolder().resolve("workspace.conf")
      val schemaRepository =
         FileWorkspaceConfigLoader(
            configFile.toPath(),
            eventDispatcher = ProjectStoreLifecycleManager(),
            projectManager = mock { })

      schemaRepository.load()
         .git?.repositories?.should?.be?.empty

      createFourRepositories(schemaRepository)
      schemaRepository.shouldHaveRepositories(fileRepoCount = 2, gitRepoCount = 2)
      assertFailsWith<Exception> {
         schemaRepository.removeFileRepository(configFile.toPath(), PackageIdentifier.fromId("com/bad/1.0.0"))
      }
      schemaRepository.shouldHaveRepositories(fileRepoCount = 2, gitRepoCount = 2)

   }

   @Test
   fun `throws error removing git repository that doesn't exist`() {
      val configFile = folder.newFolder().resolve("workspace.conf")
      val schemaRepository =
         FileWorkspaceConfigLoader(
            configFile.toPath(),
            eventDispatcher = ProjectStoreLifecycleManager(),
            projectManager = mock { })

      schemaRepository.load()
         .git?.repositories?.should?.be?.empty

      createFourRepositories(schemaRepository)
      schemaRepository.shouldHaveRepositories(fileRepoCount = 2, gitRepoCount = 2)
      assertFailsWith<Exception> {
         schemaRepository.removeGitRepository("bad-repo-1", PackageIdentifier.fromId("com/git/1.0.0"))
      }
      schemaRepository.shouldHaveRepositories(fileRepoCount = 2, gitRepoCount = 2)

   }

   private fun createFourRepositories(schemaRepository: FileWorkspaceConfigLoader) {
      schemaRepository.addGitSpec(
         GitProjectSpec(
            "test-repo-1",
            "https://github.com/test/repo1",
            "master",
         )
      )
      schemaRepository.addGitSpec(
         GitProjectSpec(
            "test-repo-2",
            "https://github.com/test/repo2",
            "master",
         )
      )

      schemaRepository.addFileSpec(
         FileProjectSpec(
            folder.resolve("project-1/").toPath(),
            loader = TaxiPackageLoaderSpec,
            packageIdentifier = PackageIdentifier.fromId("com/foo/1.0.0")
         )
      )


      schemaRepository.addFileSpec(
         FileProjectSpec(
            folder.resolve("project-2/").toPath(),
            loader = TaxiPackageLoaderSpec,
            packageIdentifier = PackageIdentifier.fromId("com/bar/1.0.0")
         )
      )
   }

}

private fun FileWorkspaceConfigLoader.shouldHaveRepositories(fileRepoCount: Int, gitRepoCount: Int) {
   this.load()
      .git?.repositories?.shouldHaveSize(gitRepoCount)
   this.load()
      .file?.projects?.shouldHaveSize(fileRepoCount)
}


fun File.newFolder(name: String? = null): File {
   val directory = this.resolve(name ?: "tmp-${System.currentTimeMillis()}")
   directory.mkdir()
   return directory
}

fun File.newFile(): File {
   return File.createTempFile("tmp", null, this)
}
