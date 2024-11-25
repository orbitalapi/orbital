package com.orbitalhq.schemaServer.core.repositories

import com.google.common.io.Resources
import com.nhaarman.mockito_kotlin.mock
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.schemaServer.core.file.deployProject
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ProjectStoreLifecycleManager
import com.orbitalhq.schemaServer.packages.OpenApiPackageLoaderSpec
import com.orbitalhq.schemaServer.packages.PackageType
import com.orbitalhq.schemaServer.packages.TaxiPackageLoaderSpec
import com.orbitalhq.schemaServer.repositories.AddFileProjectRequest
import com.orbitalhq.schemaServer.repositories.FileProjectStoreTestRequest
import com.orbitalhq.schemaServer.repositories.git.GitProjectStoreChangeRequest
import com.orbitalhq.spring.http.BadRequestException
import com.winterbe.expekt.should
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.file.shouldExist
import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.springframework.util.LinkedMultiValueMap
import reactor.test.StepVerifier


class WorkspaceProjectsServiceTest {

   @Rule
   @JvmField
   val folder = TemporaryFolder()

   lateinit var workspaceProjectsService: WorkspaceProjectsService

   @Before
   fun setup() {
      val configFile = folder.root.resolve("workspace.conf")
      val loader = FileWorkspaceConfigLoader(
         configFile.toPath(),
         eventDispatcher = ProjectStoreLifecycleManager(),
         projectManager = mock { })
      workspaceProjectsService = WorkspaceProjectsService(loader)
   }

   @Test
   fun `testing file path for relative path finds existing project relative to workspace file`() {
      val projectHome = folder.newFolder("sample-project")
      val deployedProject = projectHome.deployProject("sample-project")
      workspaceProjectsService.testFileProjectStore(FileProjectStoreTestRequest("sample-project"))
         .block()!!
         .exists.shouldBeTrue()
   }

   @Test
   fun `testing file path for absolute path finds existing project`() {
      val projectHome = folder.newFolder("sample-project")
      val deployedProject = projectHome.deployProject("sample-project")
      workspaceProjectsService.testFileProjectStore(FileProjectStoreTestRequest(projectHome.absolutePath))
         .block()!!
         .exists.shouldBeTrue()
   }

   @Test
   fun `testing file path for relative path correctly indicates no project present`() {
      workspaceProjectsService.testFileProjectStore(FileProjectStoreTestRequest("this-doesnt-exist"))
         .block()!!
         .exists.shouldBeFalse()
   }

   @Test
   fun `testing file path for absolute path correctly indicates no project present`() {
      workspaceProjectsService.testFileProjectStore(FileProjectStoreTestRequest(folder.root.resolve("this-doesnt-exist").absolutePath))
         .block()!!
         .exists.shouldBeFalse()
   }

   @Test
   fun `can add a file repository`() {
      workspaceProjectsService.listRepositories()
         .file?.projects?.should?.be?.empty

      val folder = folder.newFolder("project")

      StepVerifier.create(
         workspaceProjectsService.createFileRepository(
            AddFileProjectRequest(
               folder.canonicalPath, true,
               loader = TaxiPackageLoaderSpec,
               newProjectIdentifier = PackageIdentifier.fromId("com/foo/1.0.0")
            )
         )
      ).expectNextMatches { it.status == ModifyProjectResponseStatus.Ok }
         .verifyComplete()

      val repositoryConfig = workspaceProjectsService.listRepositories()
      repositoryConfig
         .file!!.projects.should.have.size(1)

      val fileRepoPath = repositoryConfig.file!!.projects.single()
      fileRepoPath.path.toFile().canonicalPath.should.equal(folder.canonicalPath)
      fileRepoPath.isEditable.should.be.`true`
   }

   @Test
   fun `cannot add a duplicate file repository`() {
      val folder = folder.newFolder("project")

      val request = AddFileProjectRequest(
         folder.canonicalPath,
         true,
         loader = TaxiPackageLoaderSpec,
         newProjectIdentifier = PackageIdentifier.fromId("com/foo/1.0.0")
      )

      StepVerifier.create(workspaceProjectsService.createFileRepository(request))
         .expectNextMatches { it.status == ModifyProjectResponseStatus.Ok }
         .verifyComplete()

      StepVerifier.create(workspaceProjectsService.createFileRepository(request))
         .expectErrorMatches { error ->
            error is BadRequestException
            error.message!!.shouldBe(folder.canonicalPath + " already exists")
            true
         }
         .verify()
   }

   @Test
   fun `cannot add a duplicate file repository with differing editable`() {
      val folder = folder.newFolder("project")

      val request = AddFileProjectRequest(
         folder.canonicalPath, true,
         loader = TaxiPackageLoaderSpec,
         newProjectIdentifier = PackageIdentifier.fromId("com/foo/1.0.0")
      )

      StepVerifier
         .create(workspaceProjectsService.createFileRepository(request))
         .expectNextMatches {
            it.status == ModifyProjectResponseStatus.Ok
         }.verifyComplete()


      StepVerifier
         .create(workspaceProjectsService.createFileRepository(request))
         .expectErrorMatches { error ->
            error is BadRequestException
            error.message!!.shouldBe(folder.canonicalPath + " already exists")
            true
         }
         .verify()
   }

   @Test
   fun `creating a file repository where a taxi-conf doesnt exist then one is created`() {
      // use resolve, to ensure the repository creates the directory
      val folder = folder.root.resolve("project/")

      val request = AddFileProjectRequest(
         folder.canonicalPath, true,
         loader = TaxiPackageLoaderSpec,
         newProjectIdentifier = PackageIdentifier.fromId("com/foo/1.0.0")
      )

      StepVerifier.create(workspaceProjectsService.createFileRepository(request))
         .expectNextMatches { it.status == ModifyProjectResponseStatus.Ok }
         .verifyComplete()

      folder.exists().should.be.`true`
      val taxiConfFile = folder.resolve("taxi.conf")
      taxiConfFile.exists().should.be.`true`
      taxiConfFile.isFile.should.be.`true`

      val sourceFolder = folder.resolve("src")
      sourceFolder.exists().should.be.`true`
      sourceFolder.isDirectory.should.be.`true`
   }

   @Test
   fun `can add a git repository`() {
      workspaceProjectsService.listRepositories()
         .git?.repositories?.should?.be?.empty

      StepVerifier.create(
         workspaceProjectsService.createGitProjectStore(
            GitProjectStoreChangeRequest(
               "test-repo",
               "https://github.com/test/repo",
               "master",
            )
         )
      )
         .expectNextMatches { it.status == ModifyProjectResponseStatus.Ok }
         .verifyComplete()

      workspaceProjectsService.listRepositories().git!!.repositories.should.have.size(1)
      val gitRepo = workspaceProjectsService.listRepositories().git!!.repositories.single()
      gitRepo.name.should.equal("test-repo")
      gitRepo.uri.should.equal("https://github.com/test/repo")
      gitRepo.branch.should.equal("master")
   }


   @Test
   fun `can submit an open api project`() {
      val result = workspaceProjectsService.uploadProject(
         "com.foo:test:1.0.0",
         PackageType.OpenApi,
         LinkedMultiValueMap(
            mapOf(OpenApiPackageLoaderSpec::defaultNamespace.name to listOf("com.foo.test"))
         ),
         Resources.toByteArray(Resources.getResource("open-api/petstore-expanded.yaml"))
      ).block()
      result!!.status.shouldBe(ModifyProjectResponseStatus.Ok)
      val yamlFile = folder.root.resolve("orbital/workspace/projects/com/foo/test/spec.oas.yaml")
      yamlFile.shouldExist()
      yamlFile.readText().shouldBe(Resources.getResource("open-api/petstore-expanded.yaml").readText())

      val repositoryConfig = workspaceProjectsService.listRepositories()
      repositoryConfig
         .file!!.projects.should.have.size(1)
   }

   @Test
   fun `can submit an avro project`() {
      val result = workspaceProjectsService.uploadProject(
         "com.foo:test:1.0.0",
         PackageType.Avro,
         LinkedMultiValueMap(
            emptyMap()
         ),
         Resources.toByteArray(Resources.getResource("avro/addressBook.avsc"))
      ).block()
      result!!.status.shouldBe(ModifyProjectResponseStatus.Ok)
      val avroFile = folder.root.resolve("orbital/workspace/projects/com/foo/test/spec.avsc")
      avroFile.shouldExist()
      avroFile.readText().shouldBe(Resources.getResource("avro/addressBook.avsc").readText())

      val repositoryConfig = workspaceProjectsService.listRepositories()
      repositoryConfig
         .file!!.projects.should.have.size(1)
   }

   @Test
   fun `can upload a zipped taxi project`() {
      val result = workspaceProjectsService.uploadProject(
         "ignored:ignored:1.0.0",
         PackageType.Taxi,
         LinkedMultiValueMap(
            emptyMap()
         ),
         Resources.toByteArray(Resources.getResource("zipped-projects/sample-zip-without-parent-folder.zip"))
      ).block()
      result!!.status.shouldBe(ModifyProjectResponseStatus.Ok)
      val taxiConfFile = folder.root.resolve("orbital/workspace/projects/org/taxi/sample/taxi.conf")
      taxiConfFile.shouldExist()

      val repositoryConfig = workspaceProjectsService.listRepositories()
      repositoryConfig
         .file!!.projects.should.have.size(1)
   }

   @Test
   fun `can upload a zipped taxi project with parent folder`() {
      val result = workspaceProjectsService.uploadProject(
         "ignored:ignored:1.0.0",
         PackageType.Taxi,
         LinkedMultiValueMap(
            emptyMap()
         ),
         Resources.toByteArray(Resources.getResource("zipped-projects/sample-zip-with-parent-folder.zip"))
      ).block()
      result!!.status.shouldBe(ModifyProjectResponseStatus.Ok)
      val taxiConfFile = folder.root.resolve("orbital/workspace/projects/org/taxi/sample/sample-zipped-project/taxi.conf")
      taxiConfFile.shouldExist()

      val repositoryConfig = workspaceProjectsService.listRepositories()
      repositoryConfig
         .file!!.projects.should.have.size(1)
   }
}
