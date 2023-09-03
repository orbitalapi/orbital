package com.orbitalhq.schemaServer.core.repositories

import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.io.Resources
import com.jayway.awaitility.Awaitility
import com.winterbe.expekt.should
import io.kotest.matchers.collections.shouldHaveSingleElement
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.connectors.soap.SoapWsdlSourceConverter
import com.orbitalhq.schema.api.SchemaSet
import com.orbitalhq.schema.rsocket.CBORJackson
import com.orbitalhq.schemaServer.core.file.FileSystemPackageSpec
import com.orbitalhq.schemaServer.core.file.packages.FileSystemPackageLoaderFactory
import com.orbitalhq.schemaServer.core.git.GitRepositoryConfig
import com.orbitalhq.schemaServer.core.git.GitSchemaPackageLoaderFactory
import com.orbitalhq.schemaServer.core.publisher.SourceWatchingSchemaPublisher
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ReactiveRepositoryManager
import com.orbitalhq.schemaServer.core.repositories.lifecycle.RepositoryLifecycleManager
import com.orbitalhq.schemaServer.packages.OpenApiPackageLoaderSpec
import com.orbitalhq.schemaServer.packages.SoapPackageLoaderSpec
import com.orbitalhq.schemaServer.packages.TaxiPackageLoaderSpec
import com.orbitalhq.schemaServer.repositories.CreateFileRepositoryRequest
import com.orbitalhq.schemaStore.LocalValidatingSchemaStoreClient
import com.orbitalhq.schemaStore.TaxiSchemaValidator
import com.orbitalhq.schemas.readers.TaxiSourceConverter
import lang.taxi.generators.soap.SoapLanguage
import lang.taxi.packages.TaxiPackageLoader
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.concurrent.TimeUnit
import kotlin.io.path.absolutePathString
import kotlin.io.path.toPath
import kotlin.test.assertFailsWith

class FileRepositoryIntegrationTest {


   @Rule
   @JvmField
   val folder = TemporaryFolder()

   @Test
   fun `adding a file repository to an empty folder creates a taxi project`() {
      val configFile = folder.root.resolve("repositories.conf")
      val eventDispatcher = RepositoryLifecycleManager()
      val loader = FileSchemaRepositoryConfigLoader(configFile.toPath(), eventDispatcher = eventDispatcher)

      val projectFolder = folder.newFolder().toPath()
      loader.addFileSpec(
         FileSystemPackageSpec(
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

      val configFile = folder.root.resolve("repositories.conf")
      val eventDispatcher = RepositoryLifecycleManager()
      val loader = FileSchemaRepositoryConfigLoader(configFile.toPath(), eventDispatcher = eventDispatcher)

      loader.addFileSpec(
         FileSystemPackageSpec(
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
      val (repositoryService, repositoryManager, schemaClient) = setupServices()

      // First, create the new repository
      val projectFolder = folder.newFolder("my-project")
      val targetFile = projectFolder.resolve("src/country-info.wsdl")
      targetFile.parentFile.mkdirs()
      targetFile.createNewFile()
      Resources.copy(Resources.getResource("soap/TrimmedCountryInfoServiceSpec.wsdl"), targetFile.outputStream())


      repositoryService.createFileRepository(
         CreateFileRepositoryRequest(
            projectFolder.canonicalPath,
            true,
            loader = SoapPackageLoaderSpec(
               PackageIdentifier.fromId("com/foo/1.0.0")
            ),
            newProjectIdentifier = PackageIdentifier.fromId("com/foo/1.0.0")
         )
      )

      repositoryManager.fileLoaders.should.have.size(1)
      schemaClient.schema()
         .hasType("Hello")
         .should.be.`false`

      Awaitility.await()
         .atMost(2, TimeUnit.SECONDS)
         .until<Boolean> {
            schemaClient.schema()
               .services.isNotEmpty()
         }
      val schema = schemaClient.schema()
      schema.services.shouldHaveSize(1)
      val service = schema.services.single()
      service.sourceCode.shouldHaveSingleElement { it.language == SoapLanguage.WSDL }
   }

   @Test
   fun `configure a file repository at runtime and when files are changes then schema updates are emitted`() {
      val (repositoryService, repositoryManager, schemaClient) = setupServices()

      // First, create the new repository
      val projectFolder = folder.newFolder("my-project")
      repositoryService.createFileRepository(
         CreateFileRepositoryRequest(
            projectFolder.canonicalPath,
            true,
            loader = TaxiPackageLoaderSpec,
            newProjectIdentifier = PackageIdentifier.fromId("com/foo/1.0.0")
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

   private fun setupServices(): Triple<RepositoryService, ReactiveRepositoryManager, LocalValidatingSchemaStoreClient> {
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
         eventDispatcher, eventDispatcher, eventDispatcher
      )

      // Setup: A SchemaStoreClient, which will
      // compile the taxi as it's discovered / changed
      val schemaClient = LocalValidatingSchemaStoreClient(
         schemaValidator = TaxiSchemaValidator(
            listOf(
               TaxiSourceConverter,
               SoapWsdlSourceConverter
            )
         )

      )
      val sourceWatchingSchemaPublisher = SourceWatchingSchemaPublisher(
         schemaClient,
         eventDispatcher
      )
      return Triple(repositoryService, repositoryManager, schemaClient)
   }

   @Test
   fun `declaring projects that contain additional sources are loaded into the schema`() {
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
         eventDispatcher, eventDispatcher, eventDispatcher
      )

      // Setup: A SchemaStoreClient, which will
      // compile the taxi as it's discovered / changed
      val schemaClient = LocalValidatingSchemaStoreClient()
      val sourceWatchingSchemaPublisher = SourceWatchingSchemaPublisher(
         schemaClient,
         eventDispatcher
      )

      val path = Resources.getResource("additional-sources").toURI().toPath()
      repositoryService.createFileRepository(
         CreateFileRepositoryRequest(
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
            loader = TaxiPackageLoaderSpec,
            newProjectIdentifier = PackageIdentifier.fromId("com/foo/1.0.0")
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
         eventDispatcher, eventDispatcher, eventDispatcher
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


   @Test
   fun `can delete git repository`() {
      val configFile = folder.root.resolve("repositories.conf")
      val schemaRepository =
         FileSchemaRepositoryConfigLoader(configFile.toPath(), eventDispatcher = RepositoryLifecycleManager())

      schemaRepository.load()
         .git?.repositories?.should?.be?.empty

      createFourRepositories(schemaRepository)
      schemaRepository.shouldHaveRepositories(fileRepoCount = 2, gitRepoCount = 2)

      schemaRepository.removeGitRepository("test-repo-1", PackageIdentifier.fromId("foo/bar/1.2"))

      schemaRepository.shouldHaveRepositories(fileRepoCount = 2, gitRepoCount = 1)
   }

   @Test
   fun `can delete file repository`() {
      val configFile = folder.root.resolve("repositories.conf")
      val schemaRepository =
         FileSchemaRepositoryConfigLoader(configFile.toPath(), eventDispatcher = RepositoryLifecycleManager())

      schemaRepository.load()
         .git?.repositories?.should?.be?.empty

      createFourRepositories(schemaRepository)
      schemaRepository.shouldHaveRepositories(fileRepoCount = 2, gitRepoCount = 2)

      schemaRepository.removeFileRepository(PackageIdentifier.fromId("com/foo/1.0.0"))

      schemaRepository.shouldHaveRepositories(fileRepoCount = 1, gitRepoCount = 2)

   }

   @Test
   fun `throws error removing file repository that doesn't exist`() {
      val configFile = folder.root.resolve("repositories.conf")
      val schemaRepository =
         FileSchemaRepositoryConfigLoader(configFile.toPath(), eventDispatcher = RepositoryLifecycleManager())

      schemaRepository.load()
         .git?.repositories?.should?.be?.empty

      createFourRepositories(schemaRepository)
      schemaRepository.shouldHaveRepositories(fileRepoCount = 2, gitRepoCount = 2)
      assertFailsWith<Exception> {
         schemaRepository.removeFileRepository(PackageIdentifier.fromId("com/bad/1.0.0"))
      }
      schemaRepository.shouldHaveRepositories(fileRepoCount = 2, gitRepoCount = 2)

   }

   @Test
   fun `throws error removing git repository that doesn't exist`() {
      val configFile = folder.root.resolve("repositories.conf")
      val schemaRepository =
         FileSchemaRepositoryConfigLoader(configFile.toPath(), eventDispatcher = RepositoryLifecycleManager())

      schemaRepository.load()
         .git?.repositories?.should?.be?.empty

      createFourRepositories(schemaRepository)
      schemaRepository.shouldHaveRepositories(fileRepoCount = 2, gitRepoCount = 2)
      assertFailsWith<Exception> {
         schemaRepository.removeGitRepository("bad-repo-1", PackageIdentifier.fromId("com/git/1.0.0"))
      }
      schemaRepository.shouldHaveRepositories(fileRepoCount = 2, gitRepoCount = 2)

   }

   private fun createFourRepositories(schemaRepository: FileSchemaRepositoryConfigLoader) {
      schemaRepository.addGitSpec(
         GitRepositoryConfig(
            "test-repo-1",
            "https://github.com/test/repo1",
            "master",
         )
      )
      schemaRepository.addGitSpec(
         GitRepositoryConfig(
            "test-repo-2",
            "https://github.com/test/repo2",
            "master",
         )
      )

      schemaRepository.addFileSpec(
         FileSystemPackageSpec(
            folder.root.resolve("project-1/").toPath(),
            loader = TaxiPackageLoaderSpec,
            packageIdentifier = PackageIdentifier.fromId("com/foo/1.0.0")
         )
      )


      schemaRepository.addFileSpec(
         FileSystemPackageSpec(
            folder.root.resolve("project-2/").toPath(),
            loader = TaxiPackageLoaderSpec,
            packageIdentifier = PackageIdentifier.fromId("com/bar/1.0.0")
         )
      )
   }

}

private fun FileSchemaRepositoryConfigLoader.shouldHaveRepositories(fileRepoCount: Int, gitRepoCount: Int) {
   this.load()
      .git?.repositories?.shouldHaveSize(gitRepoCount)
   this.load()
      .file?.projects?.shouldHaveSize(fileRepoCount)
}