package com.orbitalhq.schemaServer.core.repositories

import com.nhaarman.mockito_kotlin.mock
import com.orbitalhq.PackageIdentifier
import com.orbitalhq.schemaServer.core.repositories.lifecycle.ProjectStoreLifecycleManager
import com.orbitalhq.schemaServer.packages.TaxiPackageLoaderSpec
import com.orbitalhq.schemaServer.repositories.CreateFileProjectStoreRequest
import com.orbitalhq.schemaServer.repositories.git.GitProjectStoreChangeRequest
import com.orbitalhq.schemaStore.LocalValidatingSchemaStoreClient
import com.orbitalhq.spring.http.BadRequestException
import com.winterbe.expekt.should
import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import reactor.test.StepVerifier


class WorkspaceProjectsServiceTest {

   @Rule
   @JvmField
   val folder = TemporaryFolder()

   lateinit var workspaceProjectsService: WorkspaceProjectsService

   @Before
   fun setup() {
      val configFile = folder.root.resolve("repositories.conf")
      val loader = FileWorkspaceConfigLoader(configFile.toPath(), eventDispatcher = ProjectStoreLifecycleManager(), projectManager = mock {  })
      workspaceProjectsService = WorkspaceProjectsService(loader)
   }

   @Test
   fun `can add a file repository`() {
      workspaceProjectsService.listRepositories()
         .file?.projects?.should?.be?.empty

      val folder = folder.newFolder("project")

      StepVerifier.create(
      workspaceProjectsService.createFileRepository(
         CreateFileProjectStoreRequest(
            folder.canonicalPath, true,
            loader = TaxiPackageLoaderSpec,
            newProjectIdentifier = PackageIdentifier.fromId("com/foo/1.0.0")
         )
      )).expectNextMatches { it.status == ModifyProjectResponseStatus.Ok }
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

      val request = CreateFileProjectStoreRequest(
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

      val request = CreateFileProjectStoreRequest(
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

      val request = CreateFileProjectStoreRequest(
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
      ))
         .expectNextMatches { it.status == ModifyProjectResponseStatus.Ok }
         .verifyComplete()

      workspaceProjectsService.listRepositories().git!!.repositories.should.have.size(1)
      val gitRepo = workspaceProjectsService.listRepositories().git!!.repositories.single()
      gitRepo.name.should.equal("test-repo")
      gitRepo.uri.should.equal("https://github.com/test/repo")
      gitRepo.branch.should.equal("master")
   }


}
